package ninja.skyrocketing.fuyao.musicroom.model;

import lombok.*;

import java.io.Serializable;

/**
 * 专辑
 *
 * @author skyrocketing Hong
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Album implements Serializable {

    private static final long serialVersionUID = -8508341219684417455L;
    /**
     * 专辑 id
     */
    private Integer id;
    /**
     * 专辑名
     */
    private String name;
    /**
     * 艺人
     */
    private String artist;
    /**
     * 专辑图片
     */
    private String pictureUrl;
}
