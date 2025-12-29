<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CustomerCenterTabs from '../../components/CustomerCenterTabs.vue'
import PageHeader from '../../components/PageHeader.vue'

type CustomerCenterTab = 'sellerApproval' | 'inquiries'

const route = useRoute()
const router = useRouter()
const activeTab = ref<CustomerCenterTab>('sellerApproval')

const normalizeTab = (value: unknown): CustomerCenterTab | null => {
  const tabValue = Array.isArray(value) ? value[0] : value
  if (tabValue === 'sellerApproval' || tabValue === 'inquiries') {
    return tabValue
  }
  return null
}

const syncTabFromQuery = () => {
  const resolved = normalizeTab(route.query.tab)
  if (resolved) {
    activeTab.value = resolved
    return
  }

  activeTab.value = 'sellerApproval'
  if (route.query.tab !== 'sellerApproval') {
    router.replace({ query: { ...route.query, tab: 'sellerApproval' } }).catch(() => {})
  }
}

const setActiveTab = (tab: CustomerCenterTab) => {
  if (activeTab.value === tab && route.query.tab === tab) return
  activeTab.value = tab
  if (route.query.tab === tab) return
  router.replace({ query: { ...route.query, tab } }).catch(() => {})
}

watch(
  () => route.query.tab,
  () => syncTabFromQuery(),
  { immediate: true },
)
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="고객센터" />

    <CustomerCenterTabs :model-value="activeTab" @update:model-value="setActiveTab" />

    <section v-if="activeTab === 'sellerApproval'" class="live-section">
      <div class="live-section__head">
        <h3>판매자 등록 승인</h3>
        <p class="ds-section-sub">판매자 등록 요청을 확인하고 승인합니다.</p>
      </div>
    </section>

    <section v-else class="live-section">
      <div class="live-section__head">
        <h3>문의사항 확인</h3>
        <p class="ds-section-sub">고객 문의 내역을 확인합니다.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.live-section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.live-section:first-of-type {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.live-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.live-section__head h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}
</style>
