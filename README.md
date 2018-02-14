Мы всегда стараемся делать НаБ так, что бы сервис не скомпилировался без необходимых правок,
но иногда нужно внести какие-то изменения, которые не проверишь компиляцией и/или юнит-тестами.
Эти "нюансы", на которые следует обратить внимание, собраны по возможности тут.
Но и полное следование этим заметкам ничего не гарантирует, будьте внимательны!
Если обнаружите, что Вам пришлось что-то сделать не описанное тут - добавьте.

#### [3.0]

```
Перевели основной функционал nab на Spring 5.
Старые модули на базе Guice переехали в nab-retro с сохранением номера версии 2.2.9.
nab-retro теперь deprecated, но некоторое время остается жить с новой версией и релизится независимо.

Новые модули:
- nab-core: фреймворк nab на Spring 5
- nab-hibernate: интеграция с Hibernate 5
- nab-testbase: поддержка юнит-тестов 
- nab-example: пример сервиса
```

#### [2.2.9]
```
Поднята версия jdebug, в ней корректное проставление тага апстрима для дебага 
```

#### [2.2.8]
```
Поднята версия jdebug, в ней ретраи и использование jclient-common с врапперами 
```

#### [2.2.7]
```
Изменения только для юнит-тестов.
settings в контекст кладутся, каждая по своему имени, как в продовском модуле
```

#### [2.2.6]
```
Удалена автогенерация JPA МетаМодели. Её использование не рекомендуется, в критериях пишем стрингой.
```

#### [2.2.5]
```
Основное - НаБ использует jackson сериализацию в json.
Убраны @JsonModel и JacksonJerseyMarshaller, вместо них
в dependencies нужно прописать jackson-jaxrs-json-provider,
который из коробки предоставляет @Provider c jackson-овским ObjectMapper.

Если нужно вернуть на запрос json, необходимо добавить к ресурсу аннотацию @Produces(MediaType.APPLICATION_JSON)
(желательно использовать этот подход для возврата json, вместо кастомных ObjectMapper и других способов).
Если нужен кастомный ObjectMapper, есть возможность переопределить его в сервисе, написав свой @Provider ObjectMapper-а
```

#### [2.2.4]
```
Пофикшена работа jdbc jdebug логгера (сломана в 2.2.0). Если jdebug вообще нужен, то включите настройку
jdebug.enabled=true
```

#### [2.2.3]
```
ошибочный релиз.
```

#### [2.2.2]
```
Добавлена настройка http.cache.sizeInMB, позволяющая задавать размер off-heap буфера для кеширования http ответов.
Если настройка отсутствует - все работает по-старому.
При указании размера буфера меняется формат логов запросов - добаляется hit|miss кеша, поэтому нужно исправить парсинг логов в okmeter. Пример - в xmlback.

Freemarker теперь включен в nab (так как без него в текущей реализации ничего не работает), поэтому нужно убрать его из зависимостей в своих сервисах
```

#### [2.2.1]
```
Добавил аннотацию OutOfRequestScope, которая позволяет вызывать методы, требующие сбора метрик вне контекста http запроса (к примеру крон)
Замените кастомные MethodProbingInterceptor в сервисах (вида MethodProbingWithoutRequestInterceptor) на использование этой аннотации.
```

#### [2.2.0]
```
Основное - переход на 5-ый хибер, но можно модуль с хибером не обновлять, т.е. обновить только корный НаБ.
1.Версия jdbc драйвера обновлена до 42.1.4, можно забыть про ограничения с setFetchSize.
2.Удалён GuicyHibernateLoader. Он инжектил поля в хибер-сущности. Проверьте, что в ваших Hibernate энтитях ничего не injectиться, это криво.
3.Удален задепрекейченый атрибут optional у аннотации Transactional, используйте readOnly.
4.configureEjb3Configuration больше нет, использовался только для jdebug, он теперь из коробки.
  (внимание jdbc jdebug сломан в этом релизе, пофикшено в 2.2.3)
5.Изменения из-за hibernate 5:
5.1. Custom org.hibernate.usertype. Некоторые хиберовские классы переехали в spi подпакет. Просто поправить Compile-time error.
5.2. Сиквенсы.
5.2.1.@GenericGenerator(...strategy = "sequence"...)
  имя параметра поменялось.
  @Parameter(name = "sequence", value = "hhid_sequence")
    ->
  @Parameter(name = "sequence_name", value = "hhid_sequence")
5.2.2.@SequenceGenerator
  если есть настройка hibernate.id.new_generator_mappings=false, то @SequenceGenerator будет интерпретирована как старый SequenceHiLoGenerator.
  если hibernate.id.new_generator_mappings=true (default), для сохранения логики:
  @SequenceGenerator(name = "lock-history-sequence", sequenceName = "account_lock_history_id_sequence")
    ->
  @GenericGenerator(name = "lock-history-sequence", strategy = "sequence",
     parameters = {
       @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "account_lock_history_id_sequence"),
       @Parameter(name = SequenceStyleGenerator.OPT_PARAM, value = "legacy-hilo"),
       @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "50")})
5.3.Joda. org.joda.time.contrib.hibernate.* -> org.jadira.usertype.dateandtime.joda.* (либо лучше перейти на java8 time)
  поменять зависимость на
  <groupId>org.jadira.usertype</groupId>
  <artifactId>usertype.core</artifactId>
5.3.1. При вычитывании timestamp из базы, он обрубался до миллисекунд (микросекунды отрезались),
  теперь используется округление с учетом микросекунд. Таким образом при получении даты она может отличаться на миллисекунду,
  что может оказаться критичным, если по этой дате строится соль.
5.3.2.Timezone
  Hiber3: System.getProperty("user.timezone")
  Hiber5: две таймзоны - javaZone и databaseZone
  javaZone также берется из настройки, databaseZone берется по умолчанию UTC и используется при чтении и записи из базы.
  Либо добавить @Parameter(name = "databaseZone", value = "jvm") к маппингу зонированных joda-объектов, т.е.
  @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    ->
  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime", parameters = @Parameter(name = "databaseZone", value = "jvm"))
  либо перейти на LocalDateTime/java8 (лучше)
6.Следует поставить актуальный PostgreSQLDialect для хибера
7.Добавлена генерация MetaModel для построения JPA2 Criteria - можно использовать в своих проектах JPA2 Criteria
  Автогенерация выпилена в 2.2.6. Её использование не рекомендуется, пишем стрингой.
8.Удалить если есть не нужный hibernate.cfg.xml
```

#### [2.1.0]
```
Добавлен timeout на statement в базу. Чтобы его включить, нужно добавить
  default-db.statementTimeoutMs=...

ВАЖНО: Чтобы использовать timeout, сервис должен эксклюзивно использовать логин к базе (т.е. никто кроме этого сервиса не должен ходить к базе с таким же логином),
или приложение должно ходить напрямую, НЕ через pgBouncer.

ВАЖНО2: Подумайте, нужен ли вам контроль над этим таймаутом на стороне приложения.
Лучше использовать эксклюзивный логин к базе, и настроить этот timeout на стороне базы, через ALTER ROLE;

Переименованы 3 настройки. Необходимо убрать "c3p0" из названий настроек jdbcUrl, user, password, т.е.
  default-db.c3p0.jdbcUrl
  default-db.c3p0.user
  default-db.c3p0.password
    ->
  default-db.jdbcUrl
  default-db.user
  default-db.password
для каждого дата-сурса (не только default-db)

XXX.c3p0.driverClass рекомендуется убрать. Современные драйверы вполне выводятся из префикса jdbc урла, так что явное указание не требуется.
Зато, чтобы вкрячить log4jdbc достаточно изменять jdbc урл, а не менять еще и драйвер.
```

#### [2.0.2]
```
Чистка неиспользуемого нигде кода:
  -утильные классы, cvs-билдер, деревья и т.д.
  -пакет security
  -древний хистограм-мониторинг
  -аннотация Cached
  -CallbackWithRequestScope и ThreadLocalScope
```

#### [2.0.1]
```
jdebug работает в набе из коробки, если jdebug был добавлен в сервисе самостоятельно, следует убрать его.
```

#### [2.0.0]
```
1.Работа с базой уехала в отдельный мавен модуль.
1.1.Если нужно, то подключать так
  <groupId>ru.hh.nab</groupId>
  <artifactId>nab-hibernate</artifactId>
  Версию не обязательно с корным набом синхронизировать.

1.2.Инициализация Хибера
1.2.1.
  bindDataSourceAndEntityManagerAccessor(Entity.class,...
  меняется на
  install(new HibernateModule(Entity.class,...
1.2.2.
  bindDataSourceAndEntityManagerAccessor("ro-db", ReadOnly.class, Entity.class,...
  меняется на
  install(new HibernateModule("ro-db", ReadOnly.class, Entity.class,...
1.2.3. если нужно переопределить configureEjb3Configuration, то в MyServiceHibernateModule extends HibernateModule можно сделать.

2.Версию postgresql драйвера желательно проверить, какая подтягивается в проекте. В НаБе 42.1.3. Может перекрыться. Если так, то явно прописать.
2.1.в jdbc url, везде, где есть "prepareThreshold=0" добавить и "preparedStatementCacheQueries=0",
  если prepareThreshold>=0, то preparedStatementCacheQueries можно оставить дефолтным или выставить по вкусу.
2.2.из-за баги в jdbc драйвере, нужно проверить все вызовы setFetchSize.
  Нужно их избежать как-то, или пропустить этот релиз наба до следующего с версией jdbc драйвера >= 42.1.4
2.3.Рекомендуется перейти на prepared statement в jdbc драйвере, вместо c3p0

3.зависимость от freemarker и jackson стала официально optional. Если в проекте они используются, то нужно прописать в pom.xml

4.Guice 4.0 -> 4.1.0

5.@Transactional не работает на теле метода в Callable, переданного в GuicyAsyncExecutor
```

#### [1.8.16]
```
Удалена фича из 1.7.11 "ability to create a SessionManager"
```

#### [1.8.15]
```
Перед стартом сервиса (в самом начале), проверяется доступность базы.
```

#### [1.8.14]
```
Правки не требуются.
```

#### [1.8.13]
```
В настройках новая обязательная строчка
serviceName=<serviceName>
Нужно вписать уникальное имя своего сервиса. Используется, например, как префикс для метрик работы с базой.
```

#### [1.8.12]
```
Чуть вырезали из стектрейса в мониторинге соединений с базой
```

#### [1.8.11]
```
обновлена библиотека работы с метриками
```

#### [1.8.10]
```
В мониторинге использования соединений с базом поправлена работа с персентилями.
```

#### [1.8.9]
```
В мониторинг использования соединений с базом добавлен стектрейс.
```

#### [1.8.8]
```
Обновлена библиотека работы с метриками.
```

#### [1.8.7]
```
В аннотации Transactional задепрекейчен optional, вместо него нужно писать readOnly.
Смысл двух параметров одинаковый, логика обработки в релизе не менялась.
Если хоть один параметр в true, то оставляем "readOnly=true", иначе просто удалить параметры.
```

#### [1.8.6]
```
Read-only транзакции больше не создаются. Все рид-онли запросы (в мастер и на реплику) по отдельности, без транзакций.
В аннотации Transactional два параметра optional и readOnly об одном и том же. Достаточного любого в true.
```

#### [1.8.5]
```
По мотивам 1.8.0, только теперь пробел никогда не добавляется в тело ответа (раньше только для 204-го кода было отключено).
По идеи, новых превращений кодов ответа не ожидается, но вызовы в джавских клиентах на ожидание тела в ответе нужно проверить.
```

#### [1.8.4]
```
Пустой релиз. Правки не требуются.
```

#### [1.8.2]
```
Добавился мониторинг использования соединений до Базы. Если не интересует, то просто добавить настройку на каждый DataSource, типа:
default-db.monitoring.sendStats=false
ro-db.monitoring.sendStats=false
```

#### [1.8.1]
```
Добавили fail-fast - отпинывать запросы, которые уже наверняка не обработать.
```

#### [1.8.0]
```
Если возвращать из ресурса null (void) и 204-ый код ответа, то она превращалась в 200 ответ с пробелом в теле.
Теперь, если это 204, то превращения не происходит и пробел не добавляется.
Проверить, что все, кто ожидает 200, в этих случаях ждёт и 204.
А джава клиенты не вызывают методы вида .get(String.class), нужно дёргать просто .get(). Иначе упадёт.
Unit тесты это покрывают, если в них вызов клиента с этой строчкой есть.
```

#### [1.7.13]
```
Правки не требуются.
```

#### [1.7.12]
```
Сделан graceful shutdown.
```

#### [1.7.11]
```
Добавлена непонятная фича "ability to create a SessionManager", выпилена в 1.8.16.
```

#### [1.7.10]
```
Guice с 3 на 4 перешли.
```

#### [1.7.9]
```
Пустой релиз. Правки не требуются.
```

#### [1.7.8]
```
Обновили библиотеку таймингов.
```

#### [1.7.7]
```
Правки не требуются.
```

#### [1.7.6]
```
Перестали ставить HTTP header X-Accel-Buffering.
```

#### [1.7.5]
```
Удалены остатки Grizzly и TimedAsync.
```

#### [1.7.4]
```
Правки не требуются.
```

#### [1.7.3]
```
Правки не требуются.
```