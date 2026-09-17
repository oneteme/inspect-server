package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

@Getter
@Setter
public class BrowserConfigDto implements EventTrace {
    private String deviceDisplayResolution;
    private String deviceOrientation;
    private String deviceConnectivity;
    private String windowViewportBounds;
    private String windowZoomLevel;
    private String userLanguage;
    private String userTheme;
    private String navigationReferrer;
}