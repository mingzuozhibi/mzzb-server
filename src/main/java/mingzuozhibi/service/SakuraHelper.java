package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public abstract class SakuraHelper {

    public static boolean isExpiredSakura(Sakura sakura) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate sakuraDate = LocalDate.parse(sakura.getKey() + "-01", formatter);
        return LocalDate.now().isAfter(sakuraDate.plusMonths(3));
    }

    public static boolean isExpiredDisc(Disc disc) {
        return disc.getReleaseDate().isBefore(LocalDate.now().minusDays(7));
    }

    public static boolean noExpiredDisc(Disc disc) {
        return !isExpiredDisc(disc);
    }

    @SuppressWarnings("unchecked")
    public static List<Record> findLatestRank(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.desc("date"))
                .setMaxResults(2)
                .list();
    }

    @SuppressWarnings("unchecked")
    public static List<Record> findActiveRecords(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.asc("date"))
                .list();
    }

    public static int updateRecords(Dao dao, Disc disc, String recordsText) {
        String regex = "【(\\d{4})年 (\\d{2})月 (\\d{2})日 (\\d{2})時\\(.\\)】 ([*,\\d]{7})位";
        Pattern pattern = Pattern.compile(regex);
        String[] strings = recordsText.split("\n");
        int matchLine = 0;
        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                Integer rank = parseRank(matcher);
                if (rank == null) {
                    continue;
                }
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int date = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                LocalDate localDate = LocalDate.of(year, month, date);

                Record record = getOrCreateRecord(dao, disc, localDate);
                record.setRank(hour, rank);
                matchLine++;
            }
        }
        return matchLine;
    }

    private static Integer parseRank(Matcher matcher) {
        String rankText = matcher.group(5).replaceAll("[*,]", "");
        if (!rankText.isEmpty()) {
            return new Integer(rankText);
        }
        return null;
    }

    public static Record getOrCreateRecord(Dao dao, Disc disc, LocalDate date) {
        Record record = (Record) dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.eq("date", date))
                .uniqueResult();
        if (record == null) {
            record = new Record(disc, date);
            dao.save(record);
        }
        return record;
    }

    public static JSONArray buildRanks(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();
        List<Record> latestRank = findLatestRank(dao, disc);
        latestRank.forEach(record -> {
            for (int i = 0; i < 24; i++) {
                if (array.length() >= 5) {
                    break;
                }
                int hour = 23 - i;
                Integer rank = record.getRank(hour);
                if (rank != null) {
                    JSONObject object = new JSONObject();
                    object.put("date", record.getDate());
                    object.put("hour", String.format("%02d", hour));
                    object.put("rank", rank);
                    array.put(object);
                }
            }
        });
        return array;
    }

    public static JSONArray buildRecords(List<Record> records) {
        JSONArray array = new JSONArray();
        records.stream().sorted((o1, o2) -> {
            return o2.getDate().compareTo(o1.getDate());
        }).forEach(record -> {
            JSONObject object = new JSONObject();
            object.put("id", record.getId());
            object.put("date", record.getDate());
            object.put("todayPt", record.getTodayPt());
            object.put("totalPt", record.getTotalPt());
            getAverRank(record).ifPresent(averRank -> {
                object.put("averRank", (int) averRank);
            });
            array.put(object);
        });
        return array;
    }

    private static OptionalDouble getAverRank(Record record) {
        IntStream.Builder builder = IntStream.builder();
        for (int i = 0; i < 24; i++) {
            Integer rank = record.getRank(i);
            if (rank != null) {
                builder.add(rank);
            }
        }
        return builder.build().average();
    }

    public static void computeAndUpdateSakuraPt(Disc disc, List<Record> records) {
        AtomicReference<Integer> lastTotalPt = new AtomicReference<>(0);

        LocalDateTime japanTime = LocalDateTime.now().plusHours(1);
        LocalDate today = japanTime.toLocalDate();
        LocalDate seven = japanTime.minusDays(7).toLocalDate();
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        records.forEach(record -> {
            LocalDate recordDate = record.getDate();
            if (record.getTotalPt() != null) {
                if (lastTotalPt.get() != null) {
                    record.setTodayPt(record.getTotalPt() - lastTotalPt.get());
                    if (recordDate.equals(today)) {
                        disc.setTodayPt(record.getTodayPt());
                    }
                    if (recordDate.equals(seven)) {
                        sevenPt.set(record.getTotalPt());
                    }
                }
                lastTotalPt.set(record.getTotalPt());
            }
        });

        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay();
        if (days <= 0) {
            disc.setGuessPt(lastTotalPt.get());
        } else if (sevenPt.get() != null) {
            disc.setGuessPt((int) (lastTotalPt.get() + (lastTotalPt.get() - sevenPt.get()) / 7d * days));
        }
    }

    public static void computeAndUpdateAmazonPt(Disc disc, List<Record> records) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        AtomicReference<Double> totalPt = new AtomicReference<>(0d);

        LocalDateTime japanTime = LocalDateTime.now().plusHours(1);
        LocalDate today = japanTime.toLocalDate();
        LocalDate seven = japanTime.minusDays(7).toLocalDate();
        AtomicReference<Double> sevenPt = new AtomicReference<>();

        disc.setTodayPt(null);

        records.forEach(record -> {
            double todayPt = computeRecordPt(disc, record, lastRank);
            totalPt.set(totalPt.get() + todayPt);
            record.setTotalPt(totalPt.get().intValue());
            record.setTodayPt((int) todayPt);
            if (record.getDate().equals(today)) {
                disc.setTodayPt((int) todayPt);
            }
            if (record.getDate().equals(seven)) {
                sevenPt.set(totalPt.get());
            }
        });

        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay();
        if (days <= 0) {
            disc.setGuessPt(totalPt.get().intValue());
        } else if (sevenPt.get() != null) {
            disc.setGuessPt((int) (totalPt.get() + (totalPt.get() - sevenPt.get()) / 7 * days));
        }
        disc.setTotalPt(totalPt.get().intValue());
    }

    private static double computeRecordPt(Disc disc, Record record, AtomicReference<Integer> lastRank) {
        double recordPt = 0d;
        for (int i = 0; i < 24; i++) {
            Integer rank = record.getRank(i);
            if (rank == null) {
                rank = lastRank.get();
            } else {
                lastRank.set(rank);
            }
            if (rank != null) {
                recordPt += computeHourPt(disc, rank);
            }
        }
        return recordPt;
    }

    private static double computeHourPt(Disc disc, Integer rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computePt(150, 5.25, rank);
            case Other:
                return 0d;
            default:
                if (disc.getTitle().contains("Blu-ray")) {
                    if (rank <= 10) {
                        return computePt(100, 3.2, rank);
                    } else if (rank <= 20) {
                        return computePt(100, 3.3, rank);
                    } else if (rank <= 50) {
                        return computePt(100, 3.4, rank);
                    } else if (rank <= 100) {
                        return computePt(100, 3.6, rank);
                    } else if (rank <= 300) {
                        return computePt(100, 3.8, rank);
                    } else {
                        return computePt(100, 3.9, rank);
                    }
                } else {
                    return computePt(100, 4.2, rank);
                }
        }
    }

    private static double computePt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

}
