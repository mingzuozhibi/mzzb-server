package com.mingzuozhibi.modules.record;

import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class HourRecordEmbedded {

    @Transient
    public void setRank(int hour, Integer rank) {
        switch (hour) {
            case 0 -> setRank00(rank);
            case 1 -> setRank01(rank);
            case 2 -> setRank02(rank);
            case 3 -> setRank03(rank);
            case 4 -> setRank04(rank);
            case 5 -> setRank05(rank);
            case 6 -> setRank06(rank);
            case 7 -> setRank07(rank);
            case 8 -> setRank08(rank);
            case 9 -> setRank09(rank);
            case 10 -> setRank10(rank);
            case 11 -> setRank11(rank);
            case 12 -> setRank12(rank);
            case 13 -> setRank13(rank);
            case 14 -> setRank14(rank);
            case 15 -> setRank15(rank);
            case 16 -> setRank16(rank);
            case 17 -> setRank17(rank);
            case 18 -> setRank18(rank);
            case 19 -> setRank19(rank);
            case 20 -> setRank20(rank);
            case 21 -> setRank21(rank);
            case 22 -> setRank22(rank);
            case 23 -> setRank23(rank);
        }
    }

    @Transient
    public Integer getRank(int hour) {
        return switch (hour) {
            case 0 -> getRank00();
            case 1 -> getRank01();
            case 2 -> getRank02();
            case 3 -> getRank03();
            case 4 -> getRank04();
            case 5 -> getRank05();
            case 6 -> getRank06();
            case 7 -> getRank07();
            case 8 -> getRank08();
            case 9 -> getRank09();
            case 10 -> getRank10();
            case 11 -> getRank11();
            case 12 -> getRank12();
            case 13 -> getRank13();
            case 14 -> getRank14();
            case 15 -> getRank15();
            case 16 -> getRank16();
            case 17 -> getRank17();
            case 18 -> getRank18();
            case 19 -> getRank19();
            case 20 -> getRank20();
            case 21 -> getRank21();
            case 22 -> getRank22();
            case 23 -> getRank23();
            default -> null;
        };
    }

    private Integer rank00;
    private Integer rank01;
    private Integer rank02;
    private Integer rank03;
    private Integer rank04;
    private Integer rank05;
    private Integer rank06;
    private Integer rank07;
    private Integer rank08;
    private Integer rank09;
    private Integer rank10;
    private Integer rank11;
    private Integer rank12;
    private Integer rank13;
    private Integer rank14;
    private Integer rank15;
    private Integer rank16;
    private Integer rank17;
    private Integer rank18;
    private Integer rank19;
    private Integer rank20;
    private Integer rank21;
    private Integer rank22;
    private Integer rank23;

}
