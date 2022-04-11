package com.mingzuozhibi.modules.record;

import com.google.gson.JsonObject;
import com.mingzuozhibi.modules.disc.Disc;

import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.utils.FormatUtils.DATE_FORMATTER;

public abstract class RecordUtils {

    public static JsonObject buildRecord(Record record) {
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

    public static void computePt(Disc disc, LocalDate date, Record record0, Record record1, Record record7) {
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

    private static void computeTodayPt(Record record) {
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            record.setTodayPt(24 * computeHourPt(record.getDisc(), rank));
        });
    }

    private static void computeTotalPt(Record record0, Record record1) {
        if (record1 == null || record1.getTotalPt() == null) {
            record0.setTotalPt(record0.getTodayPt());
        } else if (record0.getTodayPt() != null) {
            record0.setTotalPt(record0.getTodayPt() + record1.getTotalPt());
        } else {
            record0.setTotalPt(record1.getTotalPt());
        }
    }

    private static void computeGuessPt(Record record0, Record record7) {
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

    public static double computeHourPt(Disc disc, double rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computeHourPt(150, 5.25, rank);
            case Auto:
            case Bluray:
                return computePtOfBD(rank);
            case Dvd:
                return computeHourPt(100, 4.2, rank);
            default:
                return 0d;
        }
    }

    private static double computePtOfBD(double rank) {
        if (rank <= 10) {
            return computeHourPt(100, 3.2, rank);
        } else if (rank <= 20) {
            return computeHourPt(100, 3.3, rank);
        } else if (rank <= 50) {
            return computeHourPt(100, 3.4, rank);
        } else if (rank <= 100) {
            return computeHourPt(100, 3.6, rank);
        } else if (rank <= 300) {
            return computeHourPt(100, 3.8, rank);
        } else {
            return computeHourPt(100, 3.9, rank);
        }
    }

    private static double computeHourPt(int div, double base, double rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

    public static Integer safeIntValue(Double value) {
        return Optional.ofNullable(value).map(Double::intValue).orElse(null);
    }
}
