package com.mingzuozhibi.service;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiscInfo {

    private String asin;
    private String type;
    private String date;
    private String title;
    private Integer rank;
    private Integer price;
    private boolean buyset;
    private boolean outOfStock;
    private boolean offTheShelf;

}
