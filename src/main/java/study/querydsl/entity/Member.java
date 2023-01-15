package study.querydsl.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
//기본 생성자 protected 설정
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//toString에서는 연관관계 필드는 안 건드리는 게 좋다 무한루프에 빠질 수 있기 때문이다.
@ToString(of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column(name="member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private Team team;

    public Member(String username){
        this(username, 0);
    }

    public Member(String username, int age){
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if(team!=null){
            changeTeam(team);
        }
    }

    private void changeTeam(Team team) {
        this.team=team;
        team.getMembers().add(this);
    }
}
