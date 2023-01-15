package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

/**
 * 이 클래스를 만들고 compileQuerydsl를 해줘야 함!
 */
@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    /**
     * 아래 어노테이션은 해당 클래스도 Q클래스로 생성해주기 위함이다. 이렇게 한 후에
     * 오른쪽에 Gradle를 누르고 "compileQuerydsl"를 더블 클릭해줘야 함
     * 이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다. 다만 DTO에 QueryDSL
     * 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
     * 그리고 이 Dto가 순수했으면 좋겠지만 QueryDsl 라이브러리에 의존적이라는 단점이 있다.
     * 이게 싫으면 Project field나 Project consturctor를 쓰면 된다.
     * */
    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
