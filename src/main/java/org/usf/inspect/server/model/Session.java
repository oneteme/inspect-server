package org.usf.inspect.server.model;

import org.usf.inspect.server.model.wrapper.*;

import java.util.List;

/**
 * Defines the contract for deprecated session wrappers that expose grouped request collections.
 */
@Deprecated(since = "v1.1")
public interface Session extends CompletableMetric {
    /**
     * Sets the internal identifier of the session.
     *
     * @param id the internal identifier to store.
     */
    void setId(String id);

    /**
     * Returns the REST requests associated with the session.
     *
     * @return the REST requests associated with the session.
     */
    List<RestRequestWrapper> getRestRequests();

    /**
     * Returns the database requests associated with the session.
     *
     * @return the database requests associated with the session.
     */
    List<DatabaseRequestWrapper> getDatabaseRequests();

    /**
     * Returns the local requests associated with the session.
     *
     * @return the local requests associated with the session.
     */
    List<LocalRequestWrapper> getLocalRequests();

    /**
     * Returns the FTP requests associated with the session.
     *
     * @return the FTP requests associated with the session.
     */
    List<FtpRequestWrapper> getFtpRequests();

    /**
     * Returns the mail requests associated with the session.
     *
     * @return the mail requests associated with the session.
     */
    List<MailRequestWrapper> getMailRequests();

    /**
     * Returns the directory requests associated with the session.
     *
     * @return the directory requests associated with the session.
     */
    List<DirectoryRequestWrapper> getLdapRequests();

    /**
     * Sets the instance identifier of the session.
     *
     * @param instanceId the instance identifier to store.
     */
    void setInstanceId(String instanceId);

    /**
     * Returns the instance identifier of the session.
     *
     * @return the instance identifier of the session.
     */
    String getInstanceId();

    /**
     * Returns the request mask associated with the session.
     *
     * @return the request mask associated with the session.
     */
    int getRequestsMask();
}
