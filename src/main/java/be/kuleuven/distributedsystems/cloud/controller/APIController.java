package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class APIController {
    private final Model model;

    private String topicId = "confirmquotes";
    private String pushEndpoint = "http://localhost:8080/api/confirmquotes";
    private String projectId = "mijnproject";
    TransportChannelProvider channelProvider;
    CredentialsProvider credentialsProvider;


    @Autowired
    public APIController(Model model) {
        this.model = model;

        //Set emulator settings
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();
        channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        credentialsProvider = NoCredentialsProvider.create();
        try {
            createPushSubscriptionExample();
        } catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }

    private void createPushSubscriptionExample()
            throws IOException {
        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(
               SubscriptionAdminSettings.newBuilder()
                       .setTransportChannelProvider(channelProvider)
                       .setCredentialsProvider(credentialsProvider)
                       .build());
        TopicName topicName = TopicName.of(projectId, topicId);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, "TheSubscription");
        PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

        //create topic
        try {

            TopicAdminClient topicAdminClient = TopicAdminClient.create(TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build());

            Topic topic = topicAdminClient.createTopic(topicName);
            System.out.println("Created topic: " + topic.getName());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        try {
            Subscription subscription = subscriptionAdminClient
                    .createSubscription(subscriptionName, topicName, pushConfig, 60);
            System.out.println("Created push subscription: " + subscription.getName());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    @PostMapping(path = "/addToCart", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Void> addToCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.add(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<Void> removeFromCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.remove(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/confirmCart")
    public ResponseEntity<Void> confirmCart(
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {

        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = null;
        try {
            //create publisher
            publisher = Publisher.newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();


            //Create message
            ByteString data = ByteString.copyFromUtf8(cartString + "\n" + AuthController.getUser().getEmail());
            PubsubMessage pubsubMessage =
                    PubsubMessage.newBuilder()
                            .setData(data)
                            .build();


            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published a message with custom attributes: " + messageId);

        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }



            List<Quote> cart = Cart.fromCookie(cartString);
            cart.clear();
            ResponseCookie cookie = Cart.toCookie(cart);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            headers.add(HttpHeaders.LOCATION, "/account");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    @PostMapping("/confirmquotes")
    public ResponseEntity<Void> subscription(@RequestBody String body){
        //Parse string
        JsonParser parser = (new JsonParser());
        JsonElement jsonRoot = parser.parse(body);
        String messageStr = jsonRoot.getAsJsonObject().get("message").getAsJsonObject().get("data").getAsString();

        String data = new String(Base64.getDecoder().decode(messageStr));

        String cartString = data.split("\n")[0];
        String mail = data.split("\n")[1];

        //Confirm cart
        List<Quote> cart = Cart.fromCookie(cartString);
        this.model.confirmQuotes(new ArrayList<>(cart), mail);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
