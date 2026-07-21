package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestUpdate;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class DirectoryRequest extends AbstractRequest {
	
	private String protocol;
	private String host;
	private int port;
    @Deprecated
	private boolean failed;

	@JsonCreator public DirectoryRequest() { }

    public DirectoryRequestSignal toRequest() {
        DirectoryRequestSignal dr = new DirectoryRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        dr.setInstanceId(getInstanceId());
        dr.setUser(getUser());
        dr.setProtocol(getProtocol());
        dr.setHost(getHost());
        dr.setPort(getPort());
        return dr;
    }

    public DirectoryRequestUpdate toCallback() {
        DirectoryRequestUpdate drc = new DirectoryRequestUpdate(getId());
        drc.setEnd(getEnd());
        drc.setFailed(isFailed());
        drc.setCommand(getCommand());
        return drc;
    }
    /*

        public DirectoryRequestUpdate toCallback() {
        DirectoryRequestUpdate drc = new DirectoryRequestUpdate(getId());
        drc.setEnd(getEnd());
        //drc.setFailed(isFailed());
        //régle de rétrocompatibilité
        // Si status est différent de 0, c'est que le client v5 l'a explicitement envoyé
        if (this.status != 0) {
            drc.setStatus(this.status);
        }
        // Sinon (on est en v4 ) on se base sur le booléen failed
        else {
            // -1003 (UNKNOWN_ERROR) si failed est true, -1000 (SUCCESS) si false
            drc.setStatus(this.failed ? -1003 : -1000);
        }
        drc.setCommand(getCommand());
        return drc;
    }
     */
}
