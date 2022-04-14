package com.mingzuozhibi.modules.connect;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Module {

    DISC_SHELFS("mzzb-disc-shelfs"),
    DISC_SPIDER("mzzb-disc-spider"),
    USER_SERVER("mzzb-user-server");

    private final String moduleName;

}
