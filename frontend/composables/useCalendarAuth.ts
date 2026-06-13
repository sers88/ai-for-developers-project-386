export const useCalendarAuth = () => {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase as string

  function redirectToGoogleCalendar() {
    window.location.href = `${apiBase}/api/calendar/google/connect`
  }

  return { redirectToGoogleCalendar }
}
