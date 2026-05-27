package com.onde.api.application.community;

import com.onde.api.application.community.dto.PostCreateRequest;
import com.onde.api.application.community.dto.PostCreateResponse;
import com.onde.api.application.community.dto.PostDeleteResponse;
import com.onde.api.config.MockS3Uploader;
import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PostImageRepository;
import com.onde.core.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MockS3Uploader s3Uploader;

    @Spy
    private Executor imageUploadExecutor = ForkJoinPool.commonPool(); // 테스트 환경용 비동기 스레드 풀 연동

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 등록 시 이미지가 3장을 초과하면 ValidationException이 발생한다.")
    void createPost_imageLimitExceeded() {
        // given
        PostCreateRequest req = PostCreateRequest.builder()
                .title("도쿄 여행")
                .content("정말 좋았어요")
                .type(PostType.REVIEW)
                .build();

        List<MultipartFile> images = List.of(
                new MockMultipartFile("img1", "1.jpg", "image/jpeg", new byte[0]),
                new MockMultipartFile("img2", "2.jpg", "image/jpeg", new byte[0]),
                new MockMultipartFile("img3", "3.jpg", "image/jpeg", new byte[0]),
                new MockMultipartFile("img4", "4.jpg", "image/jpeg", new byte[0]) // 4장
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(req, images, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("이미지는 최대 3장까지 첨부할 수 있습니다.");
    }

    @Test
    @DisplayName("게시글 등록이 정상적으로 완료되고 이미지 병렬 업로드 주소가 반환된다.")
    void createPost_success() {
        // given
        Long memberId = 1L;
        PostCreateRequest req = PostCreateRequest.builder()
                .title("도쿄 여행")
                .content("정말 좋았어요")
                .type(PostType.REVIEW)
                .build();

        List<MultipartFile> images = List.of(
                new MockMultipartFile("img1", "1.jpg", "image/jpeg", "image1".getBytes()),
                new MockMultipartFile("img2", "2.jpg", "image/jpeg", "image2".getBytes())
        );

        Member member = Member.builder()
                .id(memberId)
                .name("홍길동")
                .role(MemberRole.USER)
                .build();

        Post mockPost = Post.builder()
                .id(100L)
                .member(member)
                .title(req.getTitle())
                .content(req.getContent())
                .type(req.getType())
                .status(PostStatus.ACTIVE)
                .likeCount(0)
                .commentCount(0)
                .build();

        Mockito.when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenReturn(mockPost);
        Mockito.when(s3Uploader.upload(Mockito.any(MultipartFile.class), Mockito.eq("posts")))
                .thenReturn("https://cdn.example.com/posts/dummy-url.jpg");

        // when
        PostCreateResponse response = postService.createPost(req, images, memberId);

        // then
        assertThat(response.getPostId()).isEqualTo(100L);
        assertThat(response.getImageUrls()).hasSize(2);
        assertThat(response.getImageUrls().get(0)).startsWith("https://cdn.example.com/posts/");
    }

    @Test
    @DisplayName("본인의 게시글이 아닐 때 삭제를 시도하면 ForbiddenException이 발생한다.")
    void deletePost_notOwner() {
        // given
        Long postId = 100L;
        Long otherMemberId = 2L;

        Member author = Member.builder()
                .id(1L)
                .name("홍길동")
                .role(MemberRole.USER)
                .build();

        Post post = Post.builder()
                .id(postId)
                .member(author)
                .title("제목")
                .content("내용")
                .status(PostStatus.ACTIVE)
                .build();

        Mockito.when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(postId, otherMemberId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("본인 게시글만 작업할 수 있습니다.");
    }

    @Test
    @DisplayName("본인의 게시글 삭제 요청이 성공하면 Soft Delete되어 DELETED 상태로 변경된다.")
    void deletePost_success() {
        // given
        Long postId = 100L;
        Long memberId = 1L;

        Member author = Member.builder()
                .id(memberId)
                .name("홍길동")
                .role(MemberRole.USER)
                .build();

        Post post = Post.builder()
                .id(postId)
                .member(author)
                .title("제목")
                .content("내용")
                .status(PostStatus.ACTIVE)
                .build();

        Mockito.when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // when
        PostDeleteResponse response = postService.deletePost(postId, memberId);

        // then
        assertThat(response.getPostId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED); // JPA dirty checking을 통한 Soft delete 상태 검증
    }
}
