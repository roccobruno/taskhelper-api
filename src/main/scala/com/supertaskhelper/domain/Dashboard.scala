package com.supertaskhelper.domain

import spray.json.DefaultJsonProtocol

case class Dashboard(taskAsTP: TPTasksStats, taskAsSTH: STHTasksStats, tpMoney: ObligationToPay, sthMoney: Turnover)
case class TPTasksStats(open: Int, assigned: Int, completed: Int, closed: Int, requested: Int, waitingReview: Int)
case class STHTasksStats(open: Int, assigned: Int, completed: Int, closed: Int, requests: Int)
case class ObligationToPay(alreadyPaid: BigDecimal, alreadyApproved: BigDecimal, toPay: BigDecimal, refunded: BigDecimal)
case class Turnover(toWithdraw: BigDecimal, comingIncome: BigDecimal, comingIncomeOnceDone: BigDecimal, withdrew: BigDecimal)

object ObligationsJsonFormat extends DefaultJsonProtocol {
  implicit val obligationsFormat = jsonFormat4(ObligationToPay)
}

object TurnoverJsonFormat extends DefaultJsonProtocol {
  implicit val turnoverFormat = jsonFormat4(Turnover)
}

object STHTasksStatsJsonFormat extends DefaultJsonProtocol {
  implicit val sTHTasksStatsFormat = jsonFormat5(STHTasksStats)
}

object TPTasksStatsJsonFormat extends DefaultJsonProtocol {
  implicit val tPTasksStatsFormat = jsonFormat6(TPTasksStats)
}

object DashboardJsonFormat extends DefaultJsonProtocol {
  implicit val turnoverFormat = jsonFormat4(Turnover)
  implicit val obligationsFormat = jsonFormat4(ObligationToPay)
  implicit val tPTasksSFormat = jsonFormat6(TPTasksStats)
  implicit val sTHTasksStatsFormat = jsonFormat5(STHTasksStats)
  implicit val dashboardFormat = jsonFormat4(Dashboard)
}