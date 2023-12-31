package team.a501.rif.service.member;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import team.a501.rif.config.Jwt.JwtAuthenticationFilter;
import team.a501.rif.config.Jwt.JwtTokenProvider;
import team.a501.rif.domain.achievement.Achievement;
import team.a501.rif.domain.achievement.AchievementAcq;
import team.a501.rif.domain.achievement.AchievementCompleteChecker;
import team.a501.rif.domain.badge.Badge;
import team.a501.rif.domain.badge.BadgeAcq;
import team.a501.rif.domain.member.Member;
import team.a501.rif.domain.riflog.RifLog;
import team.a501.rif.domain.riflog.RifScore;
import team.a501.rif.dto.achievement.AchievementAcqInfo;
import team.a501.rif.dto.badge.BadgeAcqInfo;
import team.a501.rif.dto.badge.BadgeInfo;
import team.a501.rif.dto.member.*;
import team.a501.rif.dto.riflog.RifLogInfo;
import team.a501.rif.dto.riflog.RifLogSaveRequest;
import team.a501.rif.dto.riflog.RifLogSaveResponse;
import team.a501.rif.exception.ErrorCode;
import team.a501.rif.exception.RifCustomException;
import team.a501.rif.repository.achievement.AchievementRepository;
import team.a501.rif.repository.badge.BadgeRepository;
import team.a501.rif.repository.member.MemberRepository;
import team.a501.rif.service.achievement.AchievementAcqService;
import team.a501.rif.service.badge.BadgeAcqService;
import team.a501.rif.service.badge.BadgeService;
import team.a501.rif.service.riflog.RifLogService;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BadgeService badgeService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final BadgeRepository badgeRepository;
    private final BadgeAcqService badgeAcqService;
    private final AchievementRepository achievementRepository;
    private final AchievementAcqService achievementAcqService;
    private final RifLogService rifLogService;

    @Override
    public MemberResponse register(MemberRegisterRequest dto) {
        Member member = memberRepository.save(Member.builder()
                .id(dto.getId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .uid(dto.getUid())
                .name(dto.getName())
                .point(dto.getPoint())
                .exp(dto.getExp())
                .profileImgPath(Member.DEFAULT_PROFILE_IMG)
                .build());

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .point(member.getPoint())
                .exp(member.getExp())
                .profileImgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public void registerAll(List<MemberRegisterRequest> dtoList) {
        for (var e : dtoList) {
            memberRepository.save(Member.builder()
                    .id(e.getId())
                    .password(passwordEncoder.encode(e.getPassword()))
                    .uid(e.getUid())
                    .name(e.getName())
                    .point(e.getPoint())
                    .exp(e.getExp())
                    .profileImgPath(Member.DEFAULT_PROFILE_IMG)
                    .build());
        }
    }

    @Override
    public MemberResponse findByUid(String uid) {
        Member member = memberRepository
                .findByUid(uid)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .profileImgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public MemberResponse findById(String id) {
        Member member = memberRepository
                .findById(id)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .exp(member.getExp())
                .point(member.getPoint())
                .profileImgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public List<BadgeAcqInfo> findAllBadgeAcq(String memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        Map<Long, BadgeAcq> memberBadgeAcqs = member.getBadgeAcqs();
        List<Badge> allBadges = badgeRepository.findAll();

        List<BadgeAcqInfo> badgeAcqInfoList = new ArrayList<>();

        for (var e : allBadges) {

            if (memberBadgeAcqs.containsKey(e.getId())) {

                badgeAcqInfoList.add(BadgeAcqInfo.from(memberBadgeAcqs.get(e.getId())));
                continue;
            }

            badgeAcqInfoList.add(BadgeAcqInfo.from(e));
        }

        return badgeAcqInfoList;
    }

    @Override
    public List<BadgeAcqInfo> findBadgeAcqOnDisplay(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        return member
                .getBadgeAcqs()
                .values()
                .stream()
                .filter(BadgeAcq::getOnDisplay)
                .map(BadgeAcqInfo::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AchievementAcqInfo> findAllAchievementAcq(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        Map<Long, AchievementAcq> memberAchievementAcqs = member.getAchievementAcqs();
        List<Achievement> allAchievements = achievementRepository.findAll();

        List<AchievementAcqInfo> achievementAcqInfoList = new ArrayList<>();

        for (var e : allAchievements) {

            if (memberAchievementAcqs.containsKey(e.getId())) {

                achievementAcqInfoList.add(AchievementAcqInfo.from(memberAchievementAcqs.get(e.getId())));
                continue;
            }

            achievementAcqInfoList.add(AchievementAcqInfo.from(e));
        }

        return achievementAcqInfoList;
    }

    @Override
    public List<AchievementAcqInfo> findAchievementAcqOnDisplay(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        return member.getAchievementAcqs()
                .values()
                .stream()
                .filter(AchievementAcq::getOnDisplay)
                .map(AchievementAcqInfo::from)
                .collect(Collectors.toList());
    }

    private static final Integer GATCHA_COST = 100;

    @Override
    public BadgeGatchaResponse drawRandomBadge(String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        Integer balance = member.getPoint();
        if (balance < GATCHA_COST)
            throw new RifCustomException(ErrorCode.NOT_ENOUGH_POINTS);

        member.setPoint(balance - GATCHA_COST);

        // get random badge
        Badge badge = badgeService.getRandomBadge();

        // if we already have the badge
        Boolean reduplicated = member.hasBadge(badge.getId());

        // or add badgeAcq to member
        if (!reduplicated) {
            badgeAcqService.save(memberId, badge.getId());
        }

        return BadgeGatchaResponse.builder()
                .reduplicated(reduplicated)
                .remainingPoint(member.getPoint())
                .badge(BadgeInfo.from(badge))
                .build();
    }

    @Override
    public BadgeAcqInfo updateBadgeOnDisplay(String memberId, Long badgeId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        BadgeAcq badgeAcq = Optional.of(member.getBadgeAcqs().get(badgeId))
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        badgeAcq.toggleOnDisplay();

        return BadgeAcqInfo.from(badgeAcq);
    }

    @Override
    public AchievementAcqInfo updateAchievementOnDisplay(String memberId, Long achievementId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        AchievementAcq achievementAcq = Optional.of(member.getAchievementAcqs().get(achievementId))
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        achievementAcq.toggleOnDisplay();

        return AchievementAcqInfo.from(achievementAcq);
    }

    @Override
    public BadgeAcqInfo addBadgeAcq(String memberId, Long badgeId) {

        return badgeAcqService.save(memberId, badgeId);
    }

    @Override
    public AchievementAcqInfo addAchievementAcq(String memberId, Long achievementId) {
        return achievementAcqService.save(memberId, achievementId);
    }

    @Override
    public RifLogSaveResponse addRifLog(RifLogSaveRequest request) {

        log.info("MemberService.addRifLog: {}", request);

        Member member = memberRepository.findByUid(request.getUid())
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));
        log.info("Member info ={}", member);
        Integer score = RifScore.getScoreOf(request.getPlasticTotal(), request.getPlasticOk(),
                request.getRecycleTotal(), request.getRecycleOk());

        if(score.equals(-1)){
            return RifLogSaveResponse.builder()
                    .name(member.getName())
                    .point(0)
                    .exp(0)
                    .createdAt(null)
                    .plasticTotal(0)
                    .plasticOk(0)
                    .recycleTotal(0)
                    .recycleOk(0)
                    .build();
        }

        log.info("score info ={}", score);
        Integer gainedExp = score;
        Integer gainedPoint = 10 * ((score + 5) / 10);

        member.setExp(member.getExp() + gainedExp);
        member.setPoint(member.getPoint() + gainedPoint);

        RifLogInfo rifLogInfo = rifLogService.save(request, gainedExp, gainedPoint);

        checkRifLogsAndAddAchievements(member.getId());

        log.info("checkRifLogAndAddAchievements successfully worked");

        return RifLogSaveResponse.builder()
                .name(member.getName())
                .point(gainedPoint)
                .exp(gainedExp)
                .createdAt(rifLogInfo.getCreatedAt())
                .plasticTotal(rifLogInfo.getPlasticTotal())
                .plasticOk(rifLogInfo.getPlasticOk())
                .recycleTotal(rifLogInfo.getRecycleTotal())
                .recycleOk(rifLogInfo.getRecycleOk())
                .build();
    }

    public List<AchievementAcqInfo> checkRifLogsAndAddAchievements(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        List<RifLogInfo> rifLogInfoList = member.getRifLogs()
                .stream()
                .map(RifLogInfo::from)
                .collect(Collectors.toList());

        AchievementCompleteChecker checker = AchievementCompleteChecker.of(rifLogInfoList);

        Set<Long> alreadyAchieved = member.getAchievementAcqs().keySet();

        List<Achievement> achievementsToCheck = achievementRepository.findAll()
                .stream()
                .filter(a -> !alreadyAchieved.contains(a.getId()))
                .collect(Collectors.toList());

        List<AchievementAcqInfo> newlyAdded = new ArrayList<>();

        for (var e : achievementsToCheck) {

            if (checker.isCompleted(e.getAchievementTag())) {
                AchievementAcqInfo info
                        = this.addAchievementAcq(member.getId(), e.getId());
                newlyAdded.add(info);
            }
        }

        return newlyAdded;
    }

    public void deleteByUid(String uid) {

        Member member = memberRepository
                .findByUid(uid)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        // Badge.badgeAcqs에서 삭제
        for (var acq : member.getBadgeAcqs().values()) {
            acq.getBadge().removeBadgeAcq(acq);
        }

        // Achievement.achievementAcqs에서 삭제
        for (var acq : member.getAchievementAcqs().values()) {
            acq.getAchievement().removeAchievementAcq(acq);
        }

        memberRepository.delete(member);
    }

    @Override
    public void deleteById(String id) {

        Member member = memberRepository
                .findById(id)
                .orElseThrow(() -> new RifCustomException(ErrorCode.ENTITY_INSTANCE_NOT_FOUND));

        for (var acq : member.getBadgeAcqs().values()) {
            acq.getBadge().removeBadgeAcq(acq);
        }

        for (var acq : member.getAchievementAcqs().values()) {
            acq.getAchievement().removeAchievementAcq(acq);
        }

        memberRepository.delete(member);

    }

    @Override
    public MemberResponse passwordChange(HttpServletRequest request, String memberId, PasswordChangeRequest passwordChangeRequest) {
        String accessToken = jwtAuthenticationFilter.resolveToken(request);

        log.info("passwordChange info : {}", passwordChangeRequest, memberId);
        log.info("memberid ={}", memberId);
        log.info("accesstoken info ={}", accessToken);
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        log.info("Claims info = {}", claims);
        Member member = memberRepository.findById(claims.getSubject()).orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을수 없습니다."));
        log.info("Member by token info = {}", member);
        if (!memberId.equals(member.getId())) throw new BadCredentialsException("잘못된 유저입니다.");
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), member.getPassword()))
            throw new BadCredentialsException("다시 입력해주세요.");
        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getNewPasswordConfirm()))
            throw new BadCredentialsException("다시 입력해주세요.");
        Member changeMember = memberRepository.save(Member.builder()
                .id(member.getId())
                .uid(member.getUid())
                .exp(member.getExp())
                .password(passwordEncoder.encode(passwordChangeRequest.getNewPassword()))
                .name(member.getName())
                .point(member.getPoint())
                .profileImgPath(member.getProfileImgPath())
                .build());
        return MemberResponse.builder()
                .id(changeMember.getId())
                .uid(changeMember.getUid())
                .name(changeMember.getName())
                .profileImgPath(changeMember.getProfileImgPath())
                .build();
    }

    @Override
    public List<GetMembersName> getMembersName() {
        List<Member> getNameAll = memberRepository.findAll();
        List<GetMembersName> response = new ArrayList<>();
        for (Member b : getNameAll) {
            response.add(GetMembersName.builder().name(b.getName()).build());
        }
        return response;
    }

    @Override
    public List<FindMemberByName> findByName(String name) {
        List<Member> repo = memberRepository.findByNameLike("%" + name + "%");
        List<FindMemberByName> response = new ArrayList<>();
        for (Member b : repo) {
            response.add(FindMemberByName.builder()
                    .id(b.getId())
                    .name(b.getName())
                    .exp(b.getExp())
                    .imgPath(b.getProfileImgPath())
                    .build());
        }
        return response;
    }

    @Override
    public List<MemberRankingResponse> getFirst10ByOrderByExp() {

        List<MemberResponse> top10 = memberRepository
                .findFirst10ByOrderByExpDesc()
                .stream()
                .sorted((m1, m2) -> m1.getExp().equals(m2.getExp()) ? m1.getId().compareTo(m2.getId()) : 1)
                .map(MemberResponse::from)
                .collect(Collectors.toList());

        List<MemberRankingResponse> memberRankingResponses = new ArrayList<>();

        Integer rank = 1;
        for (var e :
                top10) {
            memberRankingResponses.add(MemberRankingResponse.builder()
                    .rank(rank)
                    .member(e)
                    .build());
            rank++;
        }

        return memberRankingResponses;
    }

    @Override
    public MemberResponse profileChange(MemberResponse changedProfile) {
        Member response = memberRepository.findById(changedProfile.getId()).orElseThrow(() ->
                new UsernameNotFoundException("해당하는 username 으로 멤버를 조회할 수 없습니다"));

        response.setProfileImgPath(changedProfile.getProfileImgPath());

        return MemberResponse.builder()
                .id(response.getId())
                .uid(response.getUid())
                .name(response.getName())
                .profileImgPath(response.getProfileImgPath())
                .build();
    }

    @Override
    public List<MemberRankingResponse> getFirstAllByOrderByExp(String memberId) {

        List<MemberResponse> orderedMembers = memberRepository
                .findByOrderByExpDesc()
                .stream()
                .sorted((m1, m2) -> m1.getExp().equals(m2.getExp()) ? m1.getId().compareTo(m2.getId()) : 1)
                .map(MemberResponse::from)
                .collect(Collectors.toList());

        List<MemberRankingResponse> memberRankingResponses = new ArrayList<>();

        MemberRankingResponse myRanking = null;

        Integer rank = 1;

        for (var e : orderedMembers) {

            if(rank <= 10) {
                memberRankingResponses.add(MemberRankingResponse.builder()
                        .rank(rank)
                        .member(e)
                        .build());
            }

            if(e.getId().equals(memberId)) {
                myRanking = MemberRankingResponse.builder()
                        .rank(rank)
                        .member(e)
                        .build();
            }

            rank++;
        }

        memberRankingResponses.add(myRanking);

        return memberRankingResponses;
    }


    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {

        return memberRepository
                .findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 username 으로 멤버를 조회할 수 없습니다"));
    }
}
