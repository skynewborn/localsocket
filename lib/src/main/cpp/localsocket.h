#ifndef LOCALSOCKET_LOCALSOCKET_H
#define LOCALSOCKET_LOCALSOCKET_H

#include <string>

/**
 * SOCKET_NAME must match Java-side server implementation
 * to set-up a connection successfully.
 */
#define SOCKET_NAME "com.skynewborn.android.localsocket"

class LocalSocket {
private:
    static int createClient(const char *name, int type);
    static ssize_t sendData(int socket, const char *buff, size_t len);
    
    const std::string &serviceId;
    int socketFd = -1;
    std::string response;
    
    std::string wrapRequest(const std::string &request);
    void parseResponse(const std::string &raw);
    /**
     * Reset
     */
    void reset();
    /**
     * Close socket if there is any.
     * @return 0 on success, and -1 on failure.
     */
    int closeSafely();

public:
    LocalSocket(std::string &serviceId) : serviceId(serviceId) {};
    LocalSocket(const std::string &serviceId) : serviceId(serviceId) {};
    ~LocalSocket();
    
    LocalSocket() = delete;
    LocalSocket(const LocalSocket&) = delete;
    LocalSocket& operator=(const LocalSocket&) = delete;
    
    /**
     * Send request message via socket.
     * @param request Request string.
     * @return Response string.
     */
    int process(const std::string &request);
    /**
     * Get response string for latest request.
     * @return 
     */
    const std::string& getResponse() const;
};


#endif //LOCALSOCKET_LOCALSOCKET_H
