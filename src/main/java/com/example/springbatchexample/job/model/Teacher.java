package com.example.springbatchexample.job.model;

import java.util.List;

import lombok.Data;

@Data
public class Teacher {

	private Long id;
	private String name;
	private List<String> students;

}
