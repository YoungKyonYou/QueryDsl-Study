package study.querydsl.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
//기본 생성자 protected 설정
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//toString에서는 연관관계 필드는 안 건드리는 게 좋다 무한루프에 빠질 수 있기 때문이다.
@ToString(of = {"id", "name"})
public class Team {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    //연관관계의 주인 설정, 여기가 연관관계의 주인이 아니라 연관관계의 주인은 Member의 team에 있다
    @OneToMany(mappedBy="team")
    private List<Member> members = new ArrayList<>();

    public Team(String name){
        this.name=name;
    }
}
