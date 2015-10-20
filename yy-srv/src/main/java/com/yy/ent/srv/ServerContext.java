package com.yy.ent.srv;

import com.yy.ent.protocol.Request;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2015/10/19
 * Time: 16:20
 * To change this template use File | Settings | File Templates.
 */
public class ServerContext {


    private static final Logger log = LoggerFactory.getLogger(ServerContext.class);

    private static final ThreadLocal<Context> localContext = new ThreadLocal();

    private Class<?> getGenericInterfaces(Class<?> clazz) {
        Type[] params = ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments();
        return ((Class) params[0]);
    }

    public void doFilter(Request request,
                         ChannelHandlerContext response) {
        localContext.set(new Context(request, response));
    }

    public void removeLocalContext() {
        localContext.remove();
    }

    public Request getRequest() {
        return getContext().getRequest();
    }

    public void setRequest(Request request) {
        ChannelHandlerContext response = getContext().getResponse();
        localContext.set(new Context(request, response));
    }

    public Context getContext() {
        Context context = localContext.get();
        if (context == null) {
            throw new RuntimeException("Please apply "
                    + ServerContext.class.getName()
                    + " to any request which uses servlet scopes.");
        }
        return context;
    }

    public ChannelHandlerContext getResponse() {
        return getContext().getResponse();
    }

    static class Context {

        final Request request;

        final ChannelHandlerContext response;

        Context(Request request, ChannelHandlerContext response) {
            this.request = request;
            this.response = response;
        }

        Request getRequest() {
            return request;
        }

        ChannelHandlerContext getResponse() {
            return response;
        }
    }
}