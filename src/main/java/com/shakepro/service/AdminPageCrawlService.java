package com.shakepro.service;

import com.shakepro.dto.response.admin.AdminPageTextResponse;

public interface AdminPageCrawlService {

    AdminPageTextResponse crawlPageText(String url);
}
