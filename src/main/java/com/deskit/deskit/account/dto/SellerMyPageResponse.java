package com.deskit.deskit.account.dto;

import java.util.List;

public class SellerMyPageResponse {
	private final String companyName;
	private final List<SellerManagerStatusResponse> managers;

	public SellerMyPageResponse(String companyName, List<SellerManagerStatusResponse> managers) {
		this.companyName = companyName;
		this.managers = managers;
	}

	public String getCompanyName() {
		return companyName;
	}

	public List<SellerManagerStatusResponse> getManagers() {
		return managers;
	}
}
