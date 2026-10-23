package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import org.usf.inspect.EventTrace;

@Getter
@Setter
public class BrowserConfigDto implements EventTrace {
    private String deviceDisplayResolution;
    private String deviceOrientation;
    private String deviceConnectivity;
    private Boolean saveData;
    private String windowViewportBounds;
    private String windowZoomLevel;
    private String userLanguage;
    private String userTheme;
   // private String navigationReferrer;
    private UUID sessionId;
}