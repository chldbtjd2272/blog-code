package com.blogcode.springredis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointRepository pointRepository;

    @PostMapping("/point")
    public ResponseEntity<Void> save(@RequestBody Point point) {
        pointRepository.save(point);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Void> count() {
        System.out.println(pointRepository.count());
        return ResponseEntity.ok().build();
    }

}
