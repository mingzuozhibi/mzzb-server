package com.mingzuozhibi.persist.rank;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
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

    public HourRecordEmbedded() {
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

    public Integer getRank00() {
        return rank00;
    }

    public void setRank00(Integer rank00) {
        this.rank00 = rank00;
    }

    public Integer getRank01() {
        return rank01;
    }

    public void setRank01(Integer rank01) {
        this.rank01 = rank01;
    }

    public Integer getRank02() {
        return rank02;
    }

    public void setRank02(Integer rank02) {
        this.rank02 = rank02;
    }

    public Integer getRank03() {
        return rank03;
    }

    public void setRank03(Integer rank03) {
        this.rank03 = rank03;
    }

    public Integer getRank04() {
        return rank04;
    }

    public void setRank04(Integer rank04) {
        this.rank04 = rank04;
    }

    public Integer getRank05() {
        return rank05;
    }

    public void setRank05(Integer rank05) {
        this.rank05 = rank05;
    }

    public Integer getRank06() {
        return rank06;
    }

    public void setRank06(Integer rank06) {
        this.rank06 = rank06;
    }

    public Integer getRank07() {
        return rank07;
    }

    public void setRank07(Integer rank07) {
        this.rank07 = rank07;
    }

    public Integer getRank08() {
        return rank08;
    }

    public void setRank08(Integer rank08) {
        this.rank08 = rank08;
    }

    public Integer getRank09() {
        return rank09;
    }

    public void setRank09(Integer rank09) {
        this.rank09 = rank09;
    }

    public Integer getRank10() {
        return rank10;
    }

    public void setRank10(Integer rank10) {
        this.rank10 = rank10;
    }

    public Integer getRank11() {
        return rank11;
    }

    public void setRank11(Integer rank11) {
        this.rank11 = rank11;
    }

    public Integer getRank12() {
        return rank12;
    }

    public void setRank12(Integer rank12) {
        this.rank12 = rank12;
    }

    public Integer getRank13() {
        return rank13;
    }

    public void setRank13(Integer rank13) {
        this.rank13 = rank13;
    }

    public Integer getRank14() {
        return rank14;
    }

    public void setRank14(Integer rank14) {
        this.rank14 = rank14;
    }

    public Integer getRank15() {
        return rank15;
    }

    public void setRank15(Integer rank15) {
        this.rank15 = rank15;
    }

    public Integer getRank16() {
        return rank16;
    }

    public void setRank16(Integer rank16) {
        this.rank16 = rank16;
    }

    public Integer getRank17() {
        return rank17;
    }

    public void setRank17(Integer rank17) {
        this.rank17 = rank17;
    }

    public Integer getRank18() {
        return rank18;
    }

    public void setRank18(Integer rank18) {
        this.rank18 = rank18;
    }

    public Integer getRank19() {
        return rank19;
    }

    public void setRank19(Integer rank19) {
        this.rank19 = rank19;
    }

    public Integer getRank20() {
        return rank20;
    }

    public void setRank20(Integer rank20) {
        this.rank20 = rank20;
    }

    public Integer getRank21() {
        return rank21;
    }

    public void setRank21(Integer rank21) {
        this.rank21 = rank21;
    }

    public Integer getRank22() {
        return rank22;
    }

    public void setRank22(Integer rank22) {
        this.rank22 = rank22;
    }

    public Integer getRank23() {
        return rank23;
    }

    public void setRank23(Integer rank23) {
        this.rank23 = rank23;
    }

}
