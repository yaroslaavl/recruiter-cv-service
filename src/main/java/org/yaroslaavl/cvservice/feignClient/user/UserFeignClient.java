package org.yaroslaavl.cvservice.feignClient.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yaroslaavl.cvservice.config.FeignConfig;

@FeignClient(name = "user-service", path = "/api/v1", configuration = FeignConfig.class)
public interface UserFeignClient {

    @GetMapping("/user/isApproved")
    boolean isApproved(@RequestParam("userId") String userId);
}