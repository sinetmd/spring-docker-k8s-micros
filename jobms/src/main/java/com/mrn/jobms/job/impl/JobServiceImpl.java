package com.mrn.jobms.job.impl;

import com.mrn.jobms.job.Job;
import com.mrn.jobms.job.JobRepository;
import com.mrn.jobms.job.JobService;
import com.mrn.jobms.job.clients.CompanyClient;
import com.mrn.jobms.job.clients.ReviewClient;
import com.mrn.jobms.job.dto.JobDTO;
import com.mrn.jobms.job.external.Company;
import com.mrn.jobms.job.external.Review;
import com.mrn.jobms.job.mapper.JobMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;

//    private final RestTemplate restTemplate;

    private final CompanyClient companyClient;

    private final ReviewClient reviewClient;

    private static int attempt = 0;

    public JobServiceImpl(JobRepository jobRepository, CompanyClient companyClient, ReviewClient reviewClient) {
        this.jobRepository = jobRepository;
        this.companyClient = companyClient;
        this.reviewClient = reviewClient;
    }

    @Override
//    @CircuitBreaker(name = "companyBreaker",
//                    fallbackMethod = "companyBreakerFallback")
//    @Retry(name = "companyBreaker",
//            fallbackMethod = "companyBreakerFallback")
    @RateLimiter(name = "companyBreaker",
            fallbackMethod = "companyBreakerFallback")
    public List<JobDTO> findAll() {
        System.out.println("Attempt: " + ++attempt);
        List<Job> jobs = jobRepository.findAll();

        return jobs.stream().map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<String> companyBreakerFallback(Exception e) {
        List<String> list = new ArrayList<>();
        list.add("Dummy");
        return list;
    }
    // using RestTemplate
/*    private JobDTO convertToDto(Job job) {
        // single object
        Company company = restTemplate.getForObject("http://company-service:8081/companies/" + job.getCompanyId(),
                Company.class);

        // for lists
        ResponseEntity<List<Review>> reviewResponse = restTemplate.exchange(
                "http://review-service:8083/reviews?companyId=" + job.getCompanyId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Review>>() {
                });

        List<Review> reviews = reviewResponse.getBody();

        return JobMapper.mapToJobWithCompanyDto(job, company, reviews);
    }*/

    // using open feign client
    private JobDTO convertToDto(Job job) {
        Company company = companyClient.getCompany(job.getCompanyId());
        List<Review> reviews = reviewClient.getReviews(job.getCompanyId());

        return JobMapper.mapToJobWithCompanyDto(job, company, reviews);
    }

    @Override
    public void createJob(Job job) {
        jobRepository.save(job);
    }

    @Override
    public JobDTO getJobById(Long id) {
        Job job = jobRepository.findById(id).orElse(null);

        if (job != null) {
            return convertToDto(job);

        }
        throw new NoSuchElementException("Could not find job with id: " + id);
    }

    @Override
    public boolean deleteJobById(Long id) {
        try {
            jobRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateJob(Long id, Job newJob) {

        Optional<Job> optionalJob = jobRepository.findById(id);

        if (optionalJob.isPresent()) {
            Job jobToUpdate = optionalJob.get();

            jobToUpdate.setTitle(newJob.getTitle());
            jobToUpdate.setDescription(newJob.getDescription());
            jobToUpdate.setMinSalary(newJob.getMinSalary());
            jobToUpdate.setMaxSalary(newJob.getMaxSalary());
            jobToUpdate.setLocation(newJob.getLocation());

            jobRepository.save(jobToUpdate);

            return true;
        }
        return false;
    }
}
