package mingzuozhibi.support;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
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

    public static int mergeRankText(Dao dao, Disc disc, String text) {
        String regex = "【(\\d{4})年 (\\d{2})月 (\\d{2})日 (\\d{2})時\\(.\\)】 ([*,\\d]{7})位";
        Pattern pattern = Pattern.compile(regex);
        String[] strings = text.split("\n");
        int matchLine = 0;
        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                Integer rank = parseNumber(matcher.group(5));
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

    public static int mergePtText(Dao dao, Disc disc, String text) {
        String regex = "【(\\d{4})年 (\\d{2})月 (\\d{2})日\\(.\\)】 ([*,\\d]{7})";
        Pattern pattern = Pattern.compile(regex);
        String[] strings = text.split("\n");
        int matchLine = 0;
        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                Integer totalPt = parseNumber(matcher.group(4));
                if (totalPt == null) {
                    continue;
                }
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int date = Integer.parseInt(matcher.group(3));
                LocalDate localDate = LocalDate.of(year, month, date);

                Record record = getOrCreateRecord(dao, disc, localDate);
                record.setTotalPt(totalPt);
                matchLine++;
            }
        }
        return matchLine;
    }

    private static Integer parseNumber(String text) {
        String rankText = text.replaceAll("[*,]", "");
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

    public static JSONArray buildRecords(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();
        findRecords(dao, disc, Order.desc("date")).forEach(record -> {
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

    @SuppressWarnings("unchecked")
    private static List<Record> findRecords(Dao dao, Disc disc, Order order) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(order)
                .list();
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

    public static JSONArray buildRanks(Dao dao, Disc disc) {
        JSONArray array = new JSONArray();
        findRanks(dao, disc).forEach(record -> {
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

    @SuppressWarnings("unchecked")
    public static List<Record> findRanks(Dao dao, Disc disc) {
        return dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.desc("date"))
                .setMaxResults(2)
                .list();
    }

    public static void computeAndUpdateSakuraPt(Dao dao, Disc disc) {
        AtomicReference<Integer> lastTotalPt = new AtomicReference<>(0);

        LocalDate today = LocalDateTime.now().plusHours(1).toLocalDate();
        LocalDate seven = today.minusDays(7);
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        findRecords(dao, disc, Order.asc("date")).forEach(record -> {
            if (record.getTotalPt() != null) {
                if (lastTotalPt.get() != null) {
                    int todayPt = record.getTotalPt() - lastTotalPt.get();
                    record.setTodayPt(todayPt);
                    checkToday(record, today, disc);
                    checkSeven(record, seven, sevenPt);
                }
                lastTotalPt.set(record.getTotalPt());
            }
        });

        disc.setTotalPt(lastTotalPt.get());

        updateGuessPt(disc, today, lastTotalPt.get(), sevenPt.get());
    }

    public static void computeAndUpdateAmazonPt(Dao dao, Disc disc) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        AtomicReference<Double> lastTotalPt = new AtomicReference<>(0d);

        LocalDate today = LocalDateTime.now().plusHours(1).toLocalDate();
        LocalDate seven = today.minusDays(7);
        AtomicReference<Integer> sevenPt = new AtomicReference<>();

        disc.setTodayPt(null);

        findRecords(dao, disc, Order.asc("date")).forEach(record -> {
            double todayPt = computeRecordPt(disc, record, lastRank);
            double totalPt = lastTotalPt.get() + todayPt;
            lastTotalPt.set(totalPt);
            record.setTotalPt((int) totalPt);
            record.setTodayPt((int) todayPt);
            checkToday(record, today, disc);
            checkSeven(record, seven, sevenPt);
        });

        disc.setTotalPt(lastTotalPt.get().intValue());

        updateGuessPt(disc, today, lastTotalPt.get().intValue(), sevenPt.get());
    }

    private static void checkToday(Record record, LocalDate today, Disc disc) {
        if (record.getDate().equals(today)) {
            disc.setTodayPt(record.getTodayPt());
        }
    }

    private static void checkSeven(Record record, LocalDate seven, AtomicReference<Integer> sevenPt) {
        if (record.getDate().equals(seven)) {
            sevenPt.set(record.getTotalPt());
        }
    }

    private static void updateGuessPt(Disc disc, LocalDate today, Integer totalPt, Integer sevenPt) {
        long days = disc.getReleaseDate().toEpochDay() - today.toEpochDay() - 1;
        if (days <= 0) {
            disc.setGuessPt(totalPt);
        } else if (sevenPt != null) {
            disc.setGuessPt((int) (totalPt + (totalPt - sevenPt) / 7d * days));
        }
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

    private static double computeHourPt(Disc disc, int rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computeHourPt(150, 5.25, rank);
            case Other:
                return 0d;
            default:
                if (disc.getTitle().contains("Blu-ray")) {
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
                } else {
                    return computeHourPt(100, 4.2, rank);
                }
        }
    }

    private static double computeHourPt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

    public static boolean isExpiredSakura(Sakura sakura) {
        return !noExpiredSakura(sakura);
    }

    public static boolean noExpiredSakura(Sakura sakura) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate sakuraDate = LocalDate.parse(sakura.getKey() + "-01", formatter);
        return LocalDate.now().isBefore(sakuraDate.plusMonths(3));
    }

    public static boolean isExpiredDisc(Disc disc) {
        return disc.getReleaseDate().isBefore(LocalDate.now().minusDays(7));
    }

    public static boolean noExpiredDisc(Disc disc) {
        return !isExpiredDisc(disc);
    }

}
