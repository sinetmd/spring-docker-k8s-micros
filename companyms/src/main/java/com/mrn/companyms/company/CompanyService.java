package com.mrn.companyms.company;

import com.mrn.companyms.company.dto.ReviewMessage;

import java.util.List;

public interface CompanyService {
    List<Company> getAllCompanies();

    boolean updateCompany(Long id, Company company);

    void createCompany(Company company);

    boolean deleteCompany(Long id);

    Company getCompanyById(Long id);

    void updateCompanyRating(ReviewMessage reviewMessage);
}
