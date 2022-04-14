package com.mingzuozhibi.commons.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    long pageSize;
    long currentPage;
    long totalElements;
}
