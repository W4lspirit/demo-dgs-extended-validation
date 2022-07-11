package com.example.demodgsextendedvalidation;

import com.example.demodgsextendedvalidation.generated.types.Show;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import java.util.List;
import java.util.stream.Collectors;

@DgsComponent
public class ShowsDatafetcher {
    private final List<Show> shows = List.of(

    );

    @DgsQuery
    public List<Show> showsMustSucceed(@InputArgument final String titleFilter) {
        if (titleFilter == null) {
            return shows;
        }

        return shows.stream().filter(s -> s.getTitle().contains(titleFilter)).toList();
    }

    @DgsQuery
    public List<Show> showsMustGoOn(@InputArgument final String titleFilter) {

        return shows.stream().filter(s -> s.getTitle().contains(titleFilter)).toList();
    }

}
