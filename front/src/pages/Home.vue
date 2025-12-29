<script setup lang="ts">
import { liveItems, popularProducts, popularSetups } from '../lib/home-data'
import LiveCarousel from '../components/LiveCarousel.vue'
import SetupCarousel from '../components/SetupCarousel.vue'
import ProductCarousel from '../components/ProductCarousel.vue'
import PageContainer from '../components/PageContainer.vue'
</script>

<template>
  <div class="home">
    <!-- Hero: full-bleed (NOT inside PageContainer) -->
    <section class="hero">
      <div class="hero__inner ds-container">
        <p class="hero__eyebrow">DESKIT</p>
        <h1 class="hero__title">나만의 데스크를 완성하는 방법</h1>
        <p class="hero__lede">라이브 · 셋업 · 아이템을 한 번에 둘러보세요.</p>
      </div>
      <div class="hero__glow" aria-hidden="true"></div>
    </section>

    <!-- Content: standardized 1200 layout -->
    <PageContainer>
      <div class="ds-stack-lg">
        <section>
          <div class="ds-section-head">
            <h2 class="section-title">라이브 방송</h2>
            <p class="ds-section-sub">지금 진행 중인 라이브를 만나보세요.</p>
          </div>
          <LiveCarousel :items="liveItems" />
        </section>

        <section>
          <div class="ds-section-head">
            <h2 class="section-title">인기 셋업</h2>
            <p class="ds-section-sub">다양한 스타일의 데스크 셋업을 둘러보세요.</p>
          </div>
          <SetupCarousel :items="popularSetups" />
        </section>

        <section>
          <div class="ds-section-head">
            <h2 class="section-title">인기 상품</h2>
            <p class="ds-section-sub">데스크 완성을 위한 추천 아이템.</p>
          </div>
          <ProductCarousel :items="popularProducts" />
        </section>
      </div>
    </PageContainer>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
}

/* Full-bleed hero (premium gradient + more breathing room) */
.hero {
  position: relative;
  overflow: hidden;
  padding: 56px 0 44px;
  margin-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  background:
    radial-gradient(1200px 520px at 20% -20%, rgba(34, 197, 94, 0.18), transparent 60%),
    radial-gradient(900px 420px at 110% 10%, rgba(17, 24, 39, 0.10), transparent 55%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.75), rgba(248, 249, 245, 0.85));
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.68));
}

.hero__inner {
  position: relative;
  z-index: 1;
}

.hero__glow {
  position: absolute;
  width: 640px;
  height: 640px;
  right: -220px;
  top: -320px;
  background: radial-gradient(circle, rgba(34, 197, 94, 0.16), transparent 62%);
  opacity: 0.65;
  filter: blur(2px);
  pointer-events: none;
}

.hero__eyebrow {
  margin: 0;
  font-weight: 900;
  letter-spacing: 0.12em;
  font-size: 12px;
  color: var(--text-muted);
}

.hero__title {
  margin: 10px 0 0;
  font-weight: 950;
  letter-spacing: -0.6px;
  color: var(--text-strong);
  font-size: 28px;
  line-height: 1.2;
}

.hero__lede {
  margin: 10px 0 0;
  color: var(--text-muted);
  font-weight: 700;
}

@media (max-width: 640px) {
  .hero {
    padding: 44px 0 34px;
  }

  .hero__title {
    font-size: 22px;
  }
}
</style>
