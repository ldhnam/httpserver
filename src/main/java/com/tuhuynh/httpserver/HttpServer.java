package com.tuhuynh.httpserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.tuhuynh.httpserver.core.ParserUtils.RequestMethod;
import com.tuhuynh.httpserver.core.RequestBinder.BIOHandlerMetadata;
import com.tuhuynh.httpserver.core.RequestBinder.BaseHandlerMetadata;
import com.tuhuynh.httpserver.core.RequestBinder.RequestHandlerBIO;
import com.tuhuynh.httpserver.core.ServerThreadFactory;
import com.tuhuynh.httpserver.core.bio.RequestPipeline;

import lombok.val;

public final class HttpServer {
    public static HttpServer port(final int serverPort) {
        return new HttpServer(serverPort);
    }

    private final int serverPort;
    private final Executor executor = Executors.newCachedThreadPool(
            new ServerThreadFactory("request-processor"));
    private ServerSocket serverSocket;
    private ArrayList<RequestHandlerBIO> middlewares = new ArrayList<>();
    private ArrayList<BaseHandlerMetadata<RequestHandlerBIO>> handlers = new ArrayList<>();

    private HttpServer(final int serverPort) {
        this.serverPort = serverPort;
    }

    public void addHandler(final RequestMethod method, final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(method, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void use(final RequestHandlerBIO... handlers) {
        middlewares.addAll(Arrays.stream(handlers)
                                 .collect(Collectors.toCollection(ArrayList::new)));
    }

    public void use(final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(RequestMethod.ALL, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void get(final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(RequestMethod.GET, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void post(final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(RequestMethod.POST, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void put(final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(RequestMethod.PUT, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void delete(final String path, final RequestHandlerBIO... handlers) {
        val newHandlers = new BIOHandlerMetadata(RequestMethod.DELETE, path, handlers);
        this.handlers.add(newHandlers);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), serverPort));
        System.out.println("Started HTTP Server on port " + serverPort);

        while (!serverSocket.isClosed()) {
            val socket = serverSocket.accept();
            executor.execute(new RequestPipeline(socket, middlewares, handlers));
        }
    }

    public void stop() throws IOException {
        if (!serverSocket.isClosed()) {
            serverSocket.close();
            System.out.println("Stopped HTTP Server on port " + serverPort);
        }
    }
}
