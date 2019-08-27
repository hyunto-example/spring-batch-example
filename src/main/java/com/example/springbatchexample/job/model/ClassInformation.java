package com.example.springbatchexample.job.model;

import lombok.Data;

@Data
public class ClassInformation {

	private String name;
	private Teacher teacher;
	private Integer studentCount;

	public ClassInformation(String name, Teacher teacher, Integer studentCount) {
		this.name = name;
		this.teacher = teacher;
		this.studentCount = studentCount;
	}

}
