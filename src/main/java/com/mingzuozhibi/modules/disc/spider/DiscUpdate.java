package com.mingzuozhibi.modules.disc.spider;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiscUpdate {

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
