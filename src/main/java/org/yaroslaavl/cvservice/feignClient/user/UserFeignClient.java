package org.yaroslaavl.cvservice.feignClient.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/api/v1")
public interface UserFeignClient {

    @GetMapping("/user/isApproved")
    boolean isApproved(@RequestHeader("Authorization") String token,
                              @RequestParam("userId") String userId);

}