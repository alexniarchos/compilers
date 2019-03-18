import java.io.InputStream;
import java.io.IOException;

public class part1 {

    private int lookaheadToken;
    private InputStream in;

    // priority & > ^

    public part1(InputStream in) throws IOException {
        this.in = in;
        lookaheadToken = in.read();
    }

    private void consume(int symbol) throws IOException, ParseError {
        if (lookaheadToken != symbol)
            throw new ParseError();
        lookaheadToken = in.read();
    }

    private int evalDigit(int digit){
        return digit - '0';
    }

    private int expr() throws IOException, ParseError {
        return expr2(term());
    }

    private int expr2(int cond) throws IOException, ParseError {
        if(lookaheadToken == ')' || lookaheadToken == '\n' || lookaheadToken == -1)
            return cond;
        if(lookaheadToken != '^')
            throw new ParseError();
        consume('^');
        return expr2(cond ^ term());
    }

    private int term() throws IOException, ParseError {
        int ret = term2(factor());
        return ret;
    }

    private int term2(int cond) throws IOException, ParseError {
        if(lookaheadToken == ')' || lookaheadToken == '^' || lookaheadToken == '\n' || lookaheadToken == -1)
            return cond;
        if(lookaheadToken != '&'){
            System.out.println("lookahead = "+lookaheadToken);
            throw new ParseError();
        }
            
        consume('&');
        return term2(cond&factor());
    }
    
    private int factor() throws IOException, ParseError {
        if(lookaheadToken == '('){
            consume('(');
            int ret = expr();
            consume(')');
            return ret;
        }
        if(lookaheadToken < '0' || lookaheadToken > '9')
            throw new ParseError();
        int ret = evalDigit(lookaheadToken);
        consume(lookaheadToken);
        return ret;
    }

    public int eval() throws IOException, ParseError {
        int rv = expr();
        if (lookaheadToken != '\n' && lookaheadToken != -1)
            throw new ParseError();
        return rv;
    }

    public static void main(String[] args) {
        try {
            part1 evaluate = new part1(System.in);
            System.out.println(evaluate.eval());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        catch(ParseError err){
            System.err.println(err.getMessage());
        }
    }
}
