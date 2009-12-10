package com.coremedia.contribution;

import net.unto.twitter.Api;
import net.unto.twitter.TwitterProtos;
import net.unto.twitter.methods.FriendshipExistsRequest;
import net.unto.twitter.methods.SearchRequest;
import org.apache.commons.cli.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public class FollowBySearch
{
  public static void main( String[] args )
  {
    CommandLine cmd = parseCommandLine(args);
    if (cmd != null) {
      Api api = Api.builder().username(cmd.getOptionValue("u")).password(cmd.getOptionValue("p")).build();
      TwitterProtos.RateLimitStatus status = api.rateLimitStatus().build().get();
      System.out.println("remaining "+status.getRemainingHits()+" until "+status.getResetTime());
      TwitterProtos.User self = api.showUser().id(cmd.getOptionValue("u")).build().get();
      int followers = self.getFollowersCount();
      int friends = self.getFriendsCount();
      System.out.println("for the records: "+friends +" friends and "+followers +" followers before action!");
      setStatus(cmd, api);

      int count = 0;
      SearchRequest.Builder builder = api.search(cmd.getOptionValue("s"));
      builder = builder.resultsPerPage(100);
      Set friendships = new HashSet<String>();
      for (int i = 1; i<10; i++ ) {
        builder.page(i);
        List<TwitterProtos.Results.Result> resultList = builder.build().get().getResultsList();
        count += followList(cmd, api, resultList, friendships);
      }
      System.out.println(" ==> added "+count+" friendships!");
    }
  }

  private static int followList(CommandLine cmd, Api api, List<TwitterProtos.Results.Result> resultList, Set<String> friendships) {
    int count = 0;
    for (TwitterProtos.Results.Result result : resultList) {
      String fromUser = result.getFromUser();
      if (!friendships.contains(fromUser)) {
        System.out.print("User: " + fromUser);
        System.out.println(", Message: " + result.getText());
        count = follow(cmd, api, count, fromUser);
        friendships.add(fromUser);
        System.out.println("");
      }
    }
    return count;
  }

  private static int follow(CommandLine cmd, Api api, int count, String fromUser) {
    try {
      FriendshipExistsRequest.Builder builder1 = api.friendshipExists(fromUser, cmd.getOptionValue("u"));
      boolean friendshipExists = builder1.build().get();
      if (friendshipExists) {
        System.out.println("you already follow " + fromUser);
      } else {
        System.out.print("you don't follow "+ fromUser +" yet... ");
        if (cmd.hasOption("t")) {
          System.out.println("in test mode, not following!");
        } else {
            TwitterProtos.User user = api.createFriendship(fromUser).build().post();
            if (user != null) {
              System.out.println(" *** successfully following "+user.getName());
              count++;
            }
        }
      }
    } catch (Exception e) {
      System.out.println("ERROR: "+e.getMessage());
    }
    return count;
  }

  private static void setStatus(CommandLine cmd, Api api) {
    String message = cmd.getOptionValue("m");
    if (message != null) {
      TwitterProtos.Status status = api.updateStatus(message).build().post();
      if (status != null) {
        System.out.println("successfully set status to "+status.getText());
      } else {
        System.out.println("failed to set status!");
      }
    }
  }

  private static CommandLine parseCommandLine(String[] args) {
    Options options = getOptions();
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse( options, args);
      boolean fail = false;
      if (!cmd.hasOption("u")) {
        System.err.println("please enter your username");
        fail = true;
      }
      if (!cmd.hasOption("p")) {
        System.err.println("please enter your password");
        fail = true;
      }
      if (!cmd.hasOption("s")) {
        System.err.println("please enter your search term");
        fail = true;
      }
      if (fail) {
        System.exit(1);
      }

    } catch (ParseException e) {
      System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
    }
    return cmd;
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption("u", "userName", true, "Twitter user name");
    options.addOption("p", "passWord", true, "Twitter password");
    options.addOption("s", "search", true, "Search string");
    options.addOption("t", "test", false, "Test only, no follow");
    options.addOption("m", "message", true, "message to set before following");
    return options;
  }
}
