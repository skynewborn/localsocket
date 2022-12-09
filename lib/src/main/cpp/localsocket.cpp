#include "localsocket.h"

#include <android/log.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cerrno>
#include <string>

#include "nlohmann/json.hpp"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LocalSocket", __VA_ARGS__)

namespace {
    int make_sockaddr_un(const char *name, struct sockaddr_un *addr, socklen_t *len) {
        memset(addr, 0, sizeof(*addr));
        size_t nameLength = strlen(name);
        
        // Test with length+1 for the *initial* '\0'.
        if ((nameLength + 1) > sizeof(addr->sun_path)) {
            goto error;
        }
        // The path in this case is *NOT* supposed to be '\0' terminated.
        // ("man 7 unix" for the gory details.)
        addr->sun_path[0] = 0;
        memcpy(addr->sun_path + 1, name, nameLength);
        
        addr->sun_family = AF_LOCAL;
        *len = nameLength + offsetof(struct sockaddr_un, sun_path) + 1;
        return 0;
        
        error:
        return -1;
    }
    
    int socket_local_client_connect(int fd, const char *name, int /*type*/) {
        struct sockaddr_un addr;
        socklen_t len;
        int err = make_sockaddr_un(name, &addr, &len);
        
        if (err < 0) {
            goto error;
        }
        if (connect(fd,  (struct sockaddr *) &addr, len) < 0) {
            goto error;
        }
        return fd;
        
        error:
        return -1;
    }
}

int LocalSocket::createClient(const char *name, int type) {
    int fd = socket(AF_LOCAL, type, 0);
    if (fd < 0) {
        return -1;
    }
    if (socket_local_client_connect(fd, name, type) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}

ssize_t LocalSocket::sendData(int socket, const char *buff, size_t len) {
    ssize_t sent;
    if ((sent = send(socket, buff, len, 0)) < 0) {
        close(socket);
        return -1;
    }
    shutdown(socket, SHUT_WR);
    return sent;
}

std::string LocalSocket::wrapRequest(const std::string &request) {
    nlohmann::json wrapped = {
            {"id", serviceId},
            {"data", request}
    };
    return wrapped.dump();
}

void LocalSocket::parseResponse(const std::string &raw) {
    auto wrapped = nlohmann::json::parse(raw, nullptr, false, true);
    if (wrapped.is_discarded()) {
        LOGE("[LocalSocket::parseResponse] Invalid format for '%s'.", raw.c_str());
        nlohmann::json fallback = {
                {"error", "Response is not JSON!"}
        };
        response = fallback.dump();
        return;
    }
    if (serviceId == wrapped["id"] && wrapped["code"] == 0) {
        auto data = wrapped["data"];
        if (data.is_string() || data.is_object()) {
            response = data.get<std::string>();
            return;
        }
        wrapped["error"] = "Unexpected type of value for 'data'!";
    }
    LOGE("[LocalSocket::parseResponse] Parse error for %s: %s", serviceId.c_str(), raw.c_str());
    response = wrapped.dump();
}

void LocalSocket::reset() {
    response.clear();
}

int LocalSocket::closeSafely() {
    int err = 0;
    if (socketFd >= 0) {
        err = shutdown(socketFd, SHUT_WR);
        if (err == -1) {
            LOGE("[LocalSocket] Failed to shutdown socket: %s.", strerror(errno));
        }
        err = close(socketFd);
        if (err == -1) {
            LOGE("[LocalSocket] Failed to close socket: %s.", strerror(errno));
        }
        socketFd = -1;
    }
    return err;
}

LocalSocket::~LocalSocket() {
    closeSafely();
    reset();
}

int LocalSocket::process(const std::string &request) {
    reset();
    
    if ((socketFd = createClient(SOCKET_NAME, SOCK_STREAM)) < 0) {
        LOGE("[LocalSocket] Connection error for %s: %s.", SOCKET_NAME, strerror(errno));
        return -1;
    }
    
    // Send request
    const std::string wrapped = wrapRequest(request);
    const char *req = wrapped.c_str();
    if (sendData(socketFd, req, strlen(req)) == -1) {
        LOGE("[LocalSocket] Send error for %s: %s.", SOCKET_NAME, strerror(errno));
        closeSafely();
        return -1;
    }
    
    // Receive response
    ssize_t len;
    char buff[1024];
    std::string resp;
    while ((len = recv(socketFd, &buff, 1024, 0)) > 0) {
        resp.append(reinterpret_cast<const char *>(&buff), len);
    }
    parseResponse(resp);
    
    closeSafely();
    return 0;
}

const std::string &LocalSocket::getResponse() const {
    return response;
}
