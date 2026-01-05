package com.deskit.deskit.ai.evaluate.service;

import com.deskit.deskit.account.enums.SellerGradeEnum;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SellerEvaluationEmailService {

	private static final String SENDER_EMAIL = "dyniiyeyo@naver.com";
	private final SendGrid sendGrid;

	public SellerEvaluationEmailService(@Value("${spring.sendgrid.api-key}") String apiKey) {
		this.sendGrid = new SendGrid(apiKey);
	}

	public void sendFinalResult(String toEmail, SellerGradeEnum grade, String adminComment) throws IOException {
		Email from = new Email(SENDER_EMAIL);
		Email to = new Email(toEmail);
		String subject = "[DESKIT] 판매자 심사 결과 안내";

		String commentText = adminComment == null || adminComment.isBlank()
				? "추가 코멘트가 없습니다."
				: adminComment;
		String contentBody = "<h2>판매자 심사 결과 안내</h2>"
				+ "<p>최종 심사 결과는 <strong>" + grade.name() + "</strong> 입니다.</p>"
				+ "<p>관리자 코멘트: " + escapeHtml(commentText) + "</p>"
				+ "<p>문의사항이 있으시면 고객센터로 연락해주세요.</p>";

		Content content = new Content("text/html", contentBody);
		Mail mail = new Mail(from, subject, to, content);

		Request request = new Request();
		request.setMethod(Method.POST);
		request.setEndpoint("mail/send");
		request.setBody(mail.build());

		sendGrid.api(request);
	}

	private String escapeHtml(String value) {
		String escaped = value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
		return escaped.replace("'", "&#39;");
	}
}
