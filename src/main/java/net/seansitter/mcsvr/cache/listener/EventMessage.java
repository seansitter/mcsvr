package net.seansitter.mcsvr.cache.listener;

public class EventMessage {
    public final Event event;
    public final Object data;

    private EventMessage(Event event, Object data) {
        this.event = event;
        this.data = data;
    }

    public static EventMessage newEventMessage(Event event, Object data) {
         return new EventMessage(event, data);
    }
}
