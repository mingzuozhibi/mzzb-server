package com.mingzuozhibi.modules.record;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Service
public class BaseRecordService {

    @Autowired
    private HourRecordRepository hourRecordRepository;

    @Autowired
    private DateRecordRepository dateRecordRepository;

    public JsonArray findRecords(Disc disc) {
        JsonArray array = new JsonArray();
        hourRecordRepository.findByDiscAndDate(disc, LocalDate.now())
            .ifPresent(record -> array.add(buildRecord(record)));
        dateRecordRepository.findDateRecords(disc, disc.getReleaseDate().plusDays(7))
            .forEach(record -> array.add(buildRecord(record)));
        return array;
    }

    private JsonObject buildRecord(BaseRecord record) {
        JsonObject object = new JsonObject();
        object.addProperty("id", record.getId());
        object.add("date", GSON.toJsonTree(record.getDate()));
        Optional.ofNullable(record.getAverRank()).ifPresent(rank -> {
            addDouble(object, "averRank", rank);
        });
        Optional.ofNullable(record.getTodayPt()).ifPresent(todayPt -> {
            addDouble(object, "todayPt", todayPt);
        });
        Optional.ofNullable(record.getTotalPt()).ifPresent(totalPt -> {
            object.addProperty("totalPt", totalPt.intValue());
        });
        Optional.ofNullable(record.getGuessPt()).ifPresent(guessPt -> {
            object.addProperty("guessPt", guessPt.intValue());
        });
        return object;
    }

    private void addDouble(JsonObject object, String name, Double number) {
        if (number < 10) {
            object.addProperty(name, number);
        } else {
            object.addProperty(name, number.intValue());
        }
    }

}
