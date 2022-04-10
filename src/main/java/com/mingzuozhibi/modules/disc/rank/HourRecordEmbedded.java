package com.mingzuozhibi.modules.disc.rank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
            case 0:
                setRank00(rank);
                break;
            case 1:
                setRank01(rank);
                break;
            case 2:
                setRank02(rank);
                break;
            case 3:
                setRank03(rank);
                break;
            case 4:
                setRank04(rank);
                break;
            case 5:
                setRank05(rank);
                break;
            case 6:
                setRank06(rank);
                break;
            case 7:
                setRank07(rank);
                break;
            case 8:
                setRank08(rank);
                break;
            case 9:
                setRank09(rank);
                break;
            case 10:
                setRank10(rank);
                break;
            case 11:
                setRank11(rank);
                break;
            case 12:
                setRank12(rank);
                break;
            case 13:
                setRank13(rank);
                break;
            case 14:
                setRank14(rank);
                break;
            case 15:
                setRank15(rank);
                break;
            case 16:
                setRank16(rank);
                break;
            case 17:
                setRank17(rank);
                break;
            case 18:
                setRank18(rank);
                break;
            case 19:
                setRank19(rank);
                break;
            case 20:
                setRank20(rank);
                break;
            case 21:
                setRank21(rank);
                break;
            case 22:
                setRank22(rank);
                break;
            case 23:
                setRank23(rank);
                break;
        }
    }

    @Transient
    public Integer getRank(int hour) {
        switch (hour) {
            case 0:
                return getRank00();
            case 1:
                return getRank01();
            case 2:
                return getRank02();
            case 3:
                return getRank03();
            case 4:
                return getRank04();
            case 5:
                return getRank05();
            case 6:
                return getRank06();
            case 7:
                return getRank07();
            case 8:
                return getRank08();
            case 9:
                return getRank09();
            case 10:
                return getRank10();
            case 11:
                return getRank11();
            case 12:
                return getRank12();
            case 13:
                return getRank13();
            case 14:
                return getRank14();
            case 15:
                return getRank15();
            case 16:
                return getRank16();
            case 17:
                return getRank17();
            case 18:
                return getRank18();
            case 19:
                return getRank19();
            case 20:
                return getRank20();
            case 21:
                return getRank21();
            case 22:
                return getRank22();
            case 23:
                return getRank23();
            default:
                return null;
        }
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
