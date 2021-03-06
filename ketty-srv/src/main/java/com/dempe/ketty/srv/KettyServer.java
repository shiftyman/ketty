package com.dempe.ketty.srv;

import com.dempe.ketty.mvc.ioc.KettyIOC;
import com.dempe.ketty.srv.http.HttpServerContext;
import com.dempe.ketty.srv.http.HttpServerInitializer;
import com.dempe.ketty.srv.ketty.KettyServerContext;
import com.dempe.ketty.srv.ketty.KettyServerInitializer;
import com.dempe.ketty.srv.uitl.ServerType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2015/10/15
 * Time: 16:41
 * To change this template use File | Settings | File Templates.
 */
public class KettyServer implements Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(KettyServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ServerBootstrap b;

    private DefaultEventExecutorGroup executorGroup;

    private KettyIOC cherry;

    private Builder builder;


    public KettyServer(Builder builder) {
        this.builder = builder;
        init(builder.serverType);

    }

    public void init(ServerType serverType) {
        executorGroup = new DefaultEventExecutorGroup(4, new DefaultThreadFactory("decode-worker-thread-pool"));
        ChannelInitializer channelInitializer;
        switch (serverType) {
            case KETTY_SERVER:
                channelInitializer = new KettyServerInitializer(new KettyServerContext(builder));
                break;
            case HTTP_SERVER:
                channelInitializer = new HttpServerInitializer(new HttpServerContext(builder));
                break;
            default:
                channelInitializer = new KettyServerInitializer(new KettyServerContext(builder));
        }
        init(channelInitializer);
    }

    public void start() {
        try {
            ChannelFuture f = b.bind(builder.host, builder.port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.info("server start:{}", builder.port);
        } finally {
            stop();
        }
    }


    private void init(ChannelInitializer channelInitializer) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, builder.tcpNoDelay)
                .option(ChannelOption.SO_KEEPALIVE, builder.soKeepAlive)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(channelInitializer);
    }


    public void stop() {
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
    }

    public KettyServer stopWithJVMShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }));
        return this;
    }

    public KettyServer initMVC() throws Exception {
        cherry = new KettyIOC();
        return this;
    }

    public static final class Builder {

        private String packageName = "com.dempe.ketty";

        private int port = 8888;

        private int readTimeout = 120;

        private String host = "localhost";

        private ServerType serverType = ServerType.HTTP_SERVER;

        private boolean tcpNoDelay = true;

        private boolean soKeepAlive = true;


        public String getPackageName() {
            return packageName;
        }

        public int getPort() {
            return port;
        }

        public ServerType getServerType() {
            return serverType;
        }

        public String getHost() {
            return host;
        }

        public Builder readTimeout(int readTimeout) {
            LOGGER.info("set readTimeout:{}", readTimeout);
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder initPackage(String packageName) {
            LOGGER.info("set scan package:{}", packageName);
            this.packageName = packageName;
            return this;
        }

        public Builder tcpNoDelay(boolean tcpNoDelay) {
            LOGGER.info("set tcp_no_delay:{}", tcpNoDelay);
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        public Builder soKeepAlive(boolean soKeepAlive) {
            LOGGER.info("set so_keep_alive:{}", soKeepAlive);
            this.soKeepAlive = soKeepAlive;
            return this;
        }


        public Builder setHttpProtocol() {
            LOGGER.info("set http protocol");
            serverType = ServerType.HTTP_SERVER;
            return this;
        }


        public Builder setKettyProtocol() {
            LOGGER.info("set jetty protocol");
            serverType = ServerType.KETTY_SERVER;
            return this;
        }

        public Builder port(int port) {
            LOGGER.info("use port:{}", port);
            this.port = port;
            return this;
        }

        public Builder host(String host) {
            LOGGER.info("use host:{}", host);
            this.host = host;
            return this;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public KettyServer build() {
            return new KettyServer(this);
        }


    }
}
