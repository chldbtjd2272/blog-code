package com.blogcode.springredis;

import org.springframework.data.repository.CrudRepository;

public interface PointRepository extends CrudRepository<Point, String> {
}
