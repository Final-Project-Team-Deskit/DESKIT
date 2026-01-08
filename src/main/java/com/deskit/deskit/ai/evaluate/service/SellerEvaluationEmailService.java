package com.deskit.deskit.ai.evaluate.service;

import com.deskit.deskit.account.enums.SellerGradeEnum;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SellerEvaluationEmailService {

	private static final String SENDER_EMAIL = "dyniiyeyo@naver.com";
	private static final String ADMITTED_TEMPLATE_PATH = "admin/doc/eval_result_admitted.html";
	private static final String REJECTED_TEMPLATE_PATH = "admin/doc/eval_result_rejected.html";
	private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title>\\s*(.*?)\\s*</title>");
	private static final Pattern BODY_PATTERN = Pattern.compile("(?is)<body[^>]*>(.*)</body>");
	private static final Pattern FIRST_NON_EMPTY_LINE = Pattern.compile("(?m)^\\s*\\S.*$");
	private final SendGrid sendGrid;

	public SellerEvaluationEmailService(@Value("${spring.sendgrid.api-key}") String apiKey) {
		this.sendGrid = new SendGrid(apiKey);
	}

	public void sendFinalResult(String toEmail, SellerGradeEnum grade, String adminComment, String sellerName, Integer totalScore) throws IOException {
		Email from = new Email(SENDER_EMAIL);
		Email to = new Email(toEmail);

		EmailTemplate template = loadTemplate(grade);
		Map<String, String> replacements = buildReplacements(grade, adminComment, sellerName, totalScore);
		String subject = applyReplacements(template.subject(), replacements);
		String htmlBody = applyReplacements(template.body(), replacements);

		String plainBody = toPlainText(htmlBody);

		Mail mail = new Mail();
		mail.setFrom(from);
		mail.setSubject(subject);

		Personalization personalization = new Personalization();
		personalization.addTo(to);
		mail.addPersonalization(personalization);

		mail.addContent(new Content("text/plain", plainBody));
		mail.addContent(new Content("text/html", htmlBody));

		Request request = new Request();
		request.setMethod(Method.POST);
		request.setEndpoint("mail/send");
		request.setBody(mail.build());

		sendGrid.api(request);
	}

	private String toPlainText(String html) {
		if (html == null) return "";
		String text = html
				.replaceAll("(?is)<br\\s*/?>", "\n")
				.replaceAll("(?is)</p\\s*>", "\n\n")
				.replaceAll("(?is)<li\\s*>", " - ")
				.replaceAll("(?is)</li\\s*>", "\n")
				.replaceAll("(?s)<[^>]*>", "");
		return text.replaceAll("[\\t\\x0B\\f\\r]+", "").trim();
	}

	private EmailTemplate loadTemplate(SellerGradeEnum grade) throws IOException {
		String path = grade == SellerGradeEnum.REJECTED ? REJECTED_TEMPLATE_PATH : ADMITTED_TEMPLATE_PATH;
		String raw = Files.readString(Path.of(path), StandardCharsets.UTF_8);
		String normalized = normalizeNewlines(stripBom(raw));
		return parseTemplate(normalized);
	}

	private Map<String, String> buildReplacements(SellerGradeEnum grade, String adminComment, String sellerName, Integer totalScore) {
		String commentText = adminComment == null || adminComment.isBlank()
				? "추가 코멘트가 없습니다."
				: adminComment;
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("{{판매자명}}", safeValue(sellerName));
		replacements.put("{{총점}}", totalScore == null ? "" : totalScore.toString());
		replacements.put("{{배정그룹}}", resolveGroupLabel(grade));
		replacements.put("{{관리자종합의견}}", commentText);
		return replacements;
	}

	private String applyReplacements(String text, Map<String, String> replacements) {
		String result = text;
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private EmailTemplate parseTemplate(String content) {
		String subject = extractTitle(content);
		String body = extractBody(content);
		if (subject == null || subject.isBlank()) {
			String fallback = findFirstNonEmptyLine(content);
			subject = stripHtml(fallback).trim();
		}
		return new EmailTemplate(subject, body);
	}

	private String extractTitle(String content) {
		Matcher matcher = TITLE_PATTERN.matcher(content);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private String extractBody(String content) {
		Matcher matcher = BODY_PATTERN.matcher(content);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return content;
	}

	private String findFirstNonEmptyLine(String content) {
		Matcher matcher = FIRST_NON_EMPTY_LINE.matcher(content);
		if (matcher.find()) {
			return matcher.group();
		}
		return "";
	}

	private String normalizeNewlines(String value) {
		return value.replace("\r\n", "\n").replace("\r", "\n");
	}

	private String stripBom(String value) {
		if (value != null && value.startsWith("\uFEFF")) {
			return value.substring(1);
		}
		return value;
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}

	private String resolveGroupLabel(SellerGradeEnum grade) {
		if (grade == null) {
			return "";
		}
		return switch (grade) {
			case A -> "공식 파트너";
			case B -> "인증 판매자";
			case C -> "신규 판매자";
			default -> grade.name();
		};
	}

	private String stripHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll("(?s)<[^>]*>", "");
	}

	private record EmailTemplate(String subject, String body) {
	}
}
