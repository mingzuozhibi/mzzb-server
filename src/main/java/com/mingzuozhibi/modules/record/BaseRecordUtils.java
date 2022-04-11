package com.mingzuozhibi.modules.record;

import com.google.gson.JsonObject;
import com.mingzuozhibi.modules.disc.Disc;

import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.FormatUtils.DATE_FORMATTER;
import static com.mingzuozhibi.utils.ReCompute.computeHourPt;

public abstract class BaseRecordUtils {

    public static JsonObject buildRecord(BaseRecord record) {
        JsonObject object = new JsonObject();
        object.addProperty("id", record.getId());
        object.addProperty("date", record.getDate().format(DATE_FORMATTER));
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            object.addProperty("averRank", rank.intValue());
        });
        Optional.ofNullable(record.getTodayPt()).ifPresent(todayPt -> {
            object.addProperty("todayPt", todayPt.intValue());
        });
        Optional.ofNullable(record.getTotalPt()).ifPresent(totalPt -> {
            object.addProperty("totalPt", totalPt.intValue());
        });
        Optional.ofNullable(record.getGuessPt()).ifPresent(guessPt -> {
            object.addProperty("guessPt", guessPt.intValue());
        });
        return object;
    }

    public static void computePt(Disc disc, LocalDate date, BaseRecord record0, BaseRecord record1, BaseRecord record7) {
        if (date.isBefore(disc.getReleaseDate())) {
            computeTodayPt(record0);
            computeTotalPt(record0, record1);
            computeGuessPt(record0, record7);
        } else if (record1 != null) {
            record0.setTodayPt(null);
            record0.setTotalPt(record1.getTotalPt());
            record0.setGuessPt(record1.getGuessPt());
        }
    }

    private static void computeTodayPt(BaseRecord record) {
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            record.setTodayPt(24 * computeHourPt(record.getDisc(), rank));
        });
    }

    private static void computeTotalPt(BaseRecord record0, BaseRecord record1) {
        if (record1 == null || record1.getTotalPt() == null) {
            record0.setTotalPt(record0.getTodayPt());
        } else if (record0.getTodayPt() != null) {
            record0.setTotalPt(record0.getTodayPt() + record1.getTotalPt());
        } else {
            record0.setTotalPt(record1.getTotalPt());
        }
    }

    private static void computeGuessPt(BaseRecord record0, BaseRecord record7) {
        if (record7 == null) {
            return;
        }
        if (record0.getTotalPt() != null && record7.getTotalPt() != null) {
            double addPt = (record0.getTotalPt() - record7.getTotalPt()) / 7d;
            LocalDate releaseDate = record0.getDisc().getReleaseDate();
            LocalDate currentDate = record0.getDate();
            long days = releaseDate.toEpochDay() - currentDate.toEpochDay() - 1;
            record0.setGuessPt(record0.getTotalPt() + addPt * days);
        }
    }

}
