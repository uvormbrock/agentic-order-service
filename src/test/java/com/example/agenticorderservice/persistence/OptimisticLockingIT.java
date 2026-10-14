package com.example.agenticorderservice.persistence;
import com.example.agenticorderservice.shared.api.*;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.aopalliance.intercept.MethodInterceptor;
import static com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction;
import static org.assertj.core.api.Assertions.*;

@org.springframework.boot.test.context.SpringBootTest(webEnvironment=org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"spring.jpa.properties.hibernate.generate_statistics=true", "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.agenticorderservice.persistence.OptimisticLockingIT$VersionSqlInspector"})
@Import(OptimisticLockingIT.CompletionGateConfiguration.class)
class OptimisticLockingIT extends PostgreSqlApiTestSupport {
    @org.springframework.beans.factory.annotation.Autowired CompletionGate gate;

    @ParameterizedTest
    @CsvSource({"product,update,update,false","customer,update,update,false",
        "product,update,patch,false","customer,update,patch,false",
        "product,patch,update,false","customer,patch,update,false",
        "product,patch,patch,false","customer,patch,patch,false",
        "product,update,update,true","customer,update,update,true",
        "product,patch,update,true","customer,patch,update,true"})
    void overlappingAndDetachedStaleMutationsProtectCommittedWinner(String feature,String first,String second,boolean detached) throws Exception {
        VersionSqlInspector.statements.clear();
        UUID id=create(feature);var loaded=new CountDownLatch(2);var committed=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var winner=executor.submit(() -> {
                try { inTransaction(factory,em -> { var entity=find(em,feature,id);loaded.countDown();await(loaded);mutate(entity,first,"Winner");return null; }); }
                finally { committed.countDown(); }
            });
            var loser=executor.submit(() -> {
                try(var em=factory.createEntityManager()) {
                    var tx=em.getTransaction();tx.begin();var entity=find(em,feature,id);
                    if(detached) { em.detach(entity);tx.commit(); }
                    loaded.countDown();await(loaded);await(committed);
                    Throwable failure=catchThrowable(() -> {
                        if(detached) { inTransaction(factory,newEm -> { mutate(entity,second,"Loser");newEm.merge(entity);return null; }); }
                        else { mutate(entity,second,"Loser");tx.commit(); }
                    });
                    if(tx.isActive()) { tx.rollback(); }
                    assertThat(failure).isNotNull();assertThat(ApiExceptionHandler.classify(failure)).isEqualTo(ApiError.Code.CONCURRENT_MODIFICATION);
                }
            });
            winner.get(20,TimeUnit.SECONDS);loser.get(20,TimeUnit.SECONDS);
        }
        var row=jdbc.queryForMap("select name,active,version from "+feature+"s where id=?",id);
        assertThat(row.get("name")).isEqualTo(first.equals("update")?"Winner":"Original");
        assertThat(row.get("active")).isEqualTo(!first.equals("patch"));assertThat(row.get("version")).isEqualTo(1L);
        assertThat(VersionSqlInspector.statements).anyMatch(sql -> sql.startsWith("update "+feature+"s ") && sql.substring(sql.indexOf(" where ")).contains("version=?"));
    }
    @ParameterizedTest @CsvSource({"product","customer"})
    void sequentialInactivePatchIsNoUpdateAndNoVersionIncrement(String feature) throws Exception {
        UUID id=create(feature);var first=request("PATCH","/"+feature+"s/"+id,"{\"active\":false}");
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var repeat=request("PATCH","/"+feature+"s/"+id,"{\"active\":false}");
        assertThat(first.statusCode()).isEqualTo(200);assertThat(repeat.statusCode()).isEqualTo(200);assertThat(repeat.body()).isEqualTo(first.body());
        assertThat(stats.getEntityUpdateCount()).isZero();assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select version from "+feature+"s where id=?",Long.class,id)).isEqualTo(1);
    }
    @ParameterizedTest @CsvSource({"product","customer"})
    void actualHttpSuccessOrConflictIsDecidedByOuterTransactionCompletion(String feature) throws Exception {
        UUID id=create(feature);gate.arm(id);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> request("PUT","/"+feature+"s/"+id,body(feature,"First")));
            var second=executor.submit(() -> request("PUT","/"+feature+"s/"+id,body(feature,"Second")));
            var a=first.get(20,TimeUnit.SECONDS);var b=second.get(20,TimeUnit.SECONDS);
            assertThat(List.of(a.statusCode(),b.statusCode())).containsExactlyInAnyOrder(200,409);
            var success=a.statusCode()==200?a:b;var failed=a.statusCode()==409?a:b;
            assertError(failed,ApiError.Code.CONCURRENT_MODIFICATION);
            assertThat(request("GET","/"+feature+"s/"+id,null).body()).isEqualTo(success.body());
            assertThat(jdbc.queryForObject("select version from "+feature+"s where id=?",Long.class,id)).isEqualTo(1);
        } finally { gate.disarm(); }
    }
    private String body(String feature,String name) {
        return mapper.writeValueAsString(feature.equals("product")?Map.of("name",name,"price",22):Map.of("name",name,"email",name.toLowerCase(Locale.ROOT)+UUID.randomUUID()+"@example.com"));
    }
    private UUID create(String feature) {
        return inTransaction(factory,em -> {
            if(feature.equals("product")) {var entity=new Product("Original",Money.of("12.34"));em.persist(entity);em.flush();return entity.getId();}
            var entity=new Customer("Original",new EmailAddress(UUID.randomUUID()+"@example.com"));em.persist(entity);em.flush();return entity.getId();
        });
    }
    private Object find(EntityManager em,String feature,UUID id) { return feature.equals("product")?em.find(Product.class,id):em.find(Customer.class,id); }
    private void mutate(Object value,String mutation,String name) {
        if(value instanceof Product product) { if(mutation.equals("patch")) {product.deactivate();}else {product.update(name,Money.of("22.00"));} }
        else if(value instanceof Customer customer) {if(mutation.equals("patch")) {customer.deactivate();}else {customer.update(name,new EmailAddress(name.toLowerCase(Locale.ROOT)+UUID.randomUUID()+"@example.com"));} }
    }
    private static void await(CountDownLatch latch) {
        try { if(!latch.await(10,TimeUnit.SECONDS)) {throw new IllegalStateException("Transaction coordination deadline exceeded");} }
        catch(InterruptedException failure) {Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted transaction coordination",failure);}
    }
    public static final class VersionSqlInspector implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final java.util.List<String> statements=new java.util.concurrent.CopyOnWriteArrayList<>();
        @Override public String inspect(String sql) { statements.add(sql);return sql; }
    }
    /** Test-only rendezvous after service return, inside the actual outer transaction. */
    static final class CompletionGate {
        private volatile UUID id;
        private volatile CyclicBarrier barrier;
        void arm(UUID id) {this.barrier=new CyclicBarrier(2);this.id=id;}
        void disarm() {this.id=null;}
        void rendezvous(Object[] arguments) {
            if(arguments.length>0 && arguments[0].equals(id)) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                try {barrier.await(10,TimeUnit.SECONDS);}
                catch(InterruptedException failure) {Thread.currentThread().interrupt();throw new IllegalStateException(failure);}
                catch(BrokenBarrierException|TimeoutException failure) {throw new IllegalStateException("HTTP transaction coordination failed",failure);}
            }
        }
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class CompletionGateConfiguration {
        @Bean static CompletionGate completionGate() {return new CompletionGate();}
        @Bean static BeanPostProcessor completionRendezvous(CompletionGate gate) {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean,String name) {
                    if(bean instanceof Advised proxy && (name.equals("productApplicationService")||name.equals("customerApplicationService"))) {
                        var advisor=new DefaultPointcutAdvisor(new StaticMethodMatcherPointcut() {
                            @Override public boolean matches(java.lang.reflect.Method method,Class<?> type) {return method.getName().equals("update");}
                        },(MethodInterceptor) invocation -> {var result=invocation.proceed();gate.rendezvous(invocation.getArguments());return result;});
                        var advisors=proxy.getAdvisors();
                        for(int i=0;i<advisors.length;i++) { if(advisors[i].getAdvice() instanceof TransactionInterceptor) {proxy.addAdvisor(i+1,advisor);break;} }
                    }
                    return bean;
                }
            };
        }
    }
}
