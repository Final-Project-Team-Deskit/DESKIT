export const resolveWsUrl = (apiBase?: string, path = '/ws'): string => {
    if (apiBase) {
        try {
            const url = new URL(apiBase)
            const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
            return `${protocol}//${url.host}${path}`
        } catch (error) {
            console.warn('[ws] invalid apiBase, fallback to location', error)
        }
    }

    if (typeof window !== 'undefined') {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        return `${protocol}//${window.location.host}${path}`
    }

    return `ws://localhost${path}`
}

export const resolveSockJsUrl = (apiBase?: string, path = '/ws-public'): string => {
    if (apiBase) {
        try {
            const url = new URL(apiBase)
            const protocol = url.protocol === 'https:' ? 'https:' : 'http:'
            return `${protocol}//${url.host}${path}`
        } catch (error) {
            console.warn('[ws] invalid apiBase, fallback to location', error)
        }
    }

    if (typeof window !== 'undefined') {
        const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
        return `${protocol}//${window.location.host}${path}`
    }

    return `http://localhost${path}`
}
