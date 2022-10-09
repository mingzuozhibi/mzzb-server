package com.mingzuozhibi.modules.spider;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Content {

    private String asin;
    private String type;
    private String date;
    private String title;
    private Integer rank;
    private boolean buyset;
    private boolean logoff;

}
