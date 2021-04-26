package ninja.skyrocketing.fuyao.musicroom.model;

import lombok.*;

import java.util.ArrayList;

/**
 * @author skyrocketing Hong
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMusic {
    private ArrayList<String> topMusicList;
    private String topMusicStrings;
}
