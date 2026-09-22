import defaultRunning from './assets/default-posts/default-running.svg'
import defaultCourse from './assets/default-posts/default-course.svg'
import defaultGear from './assets/default-posts/default-gear.svg'
import defaultTip from './assets/default-posts/default-tip.svg'
import defaultGroup from './assets/default-posts/default-group.svg'
import defaultCommunity from './assets/default-posts/default-community.svg'
import defaultQuestion from './assets/default-posts/default-question.svg'

const defaultImages = {
  '러닝 인증': defaultRunning,
  '코스 추천': defaultCourse,
  '장비 후기': defaultGear,
  '러닝 팁': defaultTip,
  '조깅 팁': defaultTip,
  '질문과 답변': defaultQuestion,
  '러닝 모임': defaultGroup,
  자유게시판: defaultCommunity,
}

export const getDefaultPostImage = (categoryName) => {
  return defaultImages[categoryName] ?? defaultCommunity
}
